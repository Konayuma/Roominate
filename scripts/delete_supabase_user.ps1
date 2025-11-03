<#
PowerShell helper to lookup and delete Supabase auth user(s) by email using the Admin API.
Usage:
  # Interactive (will prompt before deleting each user)
  .\delete_supabase_user.ps1 -ProjectUrl https://yourproject.supabase.co -ServiceRole '<SERVICE_ROLE_KEY>' -Email 'insakafilmproduction@gmail.com'

  # Non-interactive force delete (no prompts)
  .\delete_supabase_user.ps1 -ProjectUrl https://yourproject.supabase.co -ServiceRole '<SERVICE_ROLE_KEY>' -Email 'insakafilmproduction@gmail.com' -Force

This script will:
 - Lookup auth users matching the email via /auth/v1/admin/users?email=...
 - Print found users (id, email_confirmed_at, created_at)
 - Delete each user (if confirmed or -Force provided)
 - Attempt to delete related rows via the REST API: profiles and email_otps
 - Re-query and print remaining users for the email

WARNING: This deletes auth records and related data. Run only with your Service Role key and after backing up any important data.
#>

param(
    [Parameter(Mandatory=$true)]
    [string]$ProjectUrl,

    [Parameter(Mandatory=$true)]
    [string]$ServiceRole,

    [Parameter(Mandatory=$true)]
    [string]$Email,

    [switch]$Force
)

function Get-AdminUsersByEmail {
    param($url, $key, $email)
    $lookupUri = "$url/auth/v1/admin/users?email=$([System.Uri]::EscapeDataString($email))"
    try {
        $resp = Invoke-RestMethod -Method Get -Uri $lookupUri -Headers @{ 'apikey' = $key; 'Authorization' = "Bearer $key"; 'Content-Type' = 'application/json' } -ErrorAction Stop
        return $resp
    } catch {
        Write-Host "Lookup failed: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

function Delete-AdminUser {
    param($url, $key, $userId)
    $deleteUri = "$url/auth/v1/admin/users/$userId"
    try {
        $resp = Invoke-RestMethod -Method Delete -Uri $deleteUri -Headers @{ 'apikey' = $key; 'Authorization' = "Bearer $key"; 'Content-Type' = 'application/json' } -ErrorAction Stop
        Write-Host "DELETE user $userId -> success" -ForegroundColor Green
        return $true
    } catch {
        # Show HTTP status if available
        if ($_.Exception.Response) {
            $status = $_.Exception.Response.StatusCode.value__
            $body = $_.Exception.Response.GetResponseStream() | ForEach-Object { $_ } | Out-String
            Write-Host "DELETE user $userId -> HTTP $status" -ForegroundColor Yellow
            Write-Host $body
        } else {
            Write-Host "DELETE user $userId failed: $($_.Exception.Message)" -ForegroundColor Red
        }
        return $false
    }
}

function Delete-RowRest {
    param($url, $key, $path)
    $uri = "$url/rest/v1/$path"
    try {
        $resp = Invoke-RestMethod -Method Delete -Uri $uri -Headers @{ 'apikey' = $key; 'Authorization' = "Bearer $key"; 'Content-Type' = 'application/json' } -ErrorAction Stop
        Write-Host "DELETE $path -> success" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "DELETE $path -> failed: $($_.Exception.Message)" -ForegroundColor Yellow
        return $false
    }
}

Write-Host "Looking up users for email: $Email" -ForegroundColor Cyan
$users = Get-AdminUsersByEmail -url $ProjectUrl -key $ServiceRole -email $Email
if (-not $users) { Write-Host "No response from lookup. Aborting." -ForegroundColor Red; exit 1 }

# If API returns an object with "users" property (some SDKs wrap results), normalize
if ($users.PSObject.Properties.Name -contains 'users') { $usersList = $users.users } else { $usersList = $users }

if (-not $usersList -or $usersList.Length -eq 0) {
    Write-Host "No users found for email $Email" -ForegroundColor Yellow
} else {
    Write-Host "Found $($usersList.Length) user(s):" -ForegroundColor Green
    $i=0
    foreach ($u in $usersList) {
        $i++
        Write-Host "[$i] id: $($u.id)  email: $($u.email)  confirmed_at: $($u.email_confirmed_at)  created_at: $($u.created_at)"
    }

    foreach ($u in $usersList) {
        $userId = $u.id
        $doDelete = $false
        if ($Force) { $doDelete = $true } else {
            $ans = Read-Host "Type DELETE to remove user $userId (or press Enter to skip)"
            if ($ans -eq 'DELETE') { $doDelete = $true }
        }

        if ($doDelete) {
            $ok = Delete-AdminUser -url $ProjectUrl -key $ServiceRole -userId $userId
n            if ($ok) {
                Write-Host "Attempting to delete profile and email_otps associated with email $Email" -ForegroundColor Cyan
                # Try delete profile by id first
                Delete-RowRest -url $ProjectUrl -key $ServiceRole -path "profiles?id=eq.$userId"
                # Also try delete profile by email
                Delete-RowRest -url $ProjectUrl -key $ServiceRole -path "profiles?email=eq.$([System.Uri]::EscapeDataString($Email))"
                # Delete OTPs by email
                Delete-RowRest -url $ProjectUrl -key $ServiceRole -path "email_otps?email=eq.$([System.Uri]::EscapeDataString($Email))"
            } else {
                Write-Host "Skipping related-row cleanup because delete failed." -ForegroundColor Yellow
            }
        } else {
            Write-Host "Skipped deletion for $userId" -ForegroundColor Yellow
        }
    }

    # Re-query to show current state
    Write-Host "\nRe-querying users for email..." -ForegroundColor Cyan
    $after = Get-AdminUsersByEmail -url $ProjectUrl -key $ServiceRole -email $Email
    if ($after.PSObject.Properties.Name -contains 'users') { $afterList = $after.users } else { $afterList = $after }
    if (-not $afterList -or $afterList.Length -eq 0) { Write-Host "No users found for email $Email after deletion." -ForegroundColor Green }
    else { Write-Host "Users still present:" -ForegroundColor Yellow; $afterList | ConvertTo-Json -Depth 5 }
}
