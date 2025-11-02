<#
PowerShell helper script to deploy Supabase Edge Functions and run basic tests.

Usage: run in repository root (where supabase/ folder exists).
    powershell -ExecutionPolicy Bypass -File .\supabase\deploy_and_test.ps1

What it does:
- Ensures supabase CLI is installed (attempts npm global install if npm exists)
- Optionally sets secrets (reads from env or prompts)
- Deploys send-otp, verify-otp, complete-signup functions
- Prints recommended invoke and log commands to run manually

Security note: This script will call `supabase secrets set` if it finds values in environment variables or if you paste them when prompted. Keep the service_role key secret.
#>

function Check-Command {
    param([string]$cmd)
    $proc = Start-Process -FilePath powershell -ArgumentList "-NoProfile -Command `"Get-Command $cmd -ErrorAction SilentlyContinue`"" -NoNewWindow -RedirectStandardOutput temp_out.txt -PassThru -Wait
    $out = Get-Content temp_out.txt -Raw
    Remove-Item temp_out.txt -ErrorAction SilentlyContinue
    return -not [string]::IsNullOrWhiteSpace($out)
}

Write-Output "== Supabase functions deploy & test helper =="

# 1) Check for supabase CLI
$hasSupabase = $false
try {
    & supabase --version > $null 2>&1
    $hasSupabase = $? 
} catch {
    $hasSupabase = $false
}

if (-not $hasSupabase) {
    Write-Output "supabase CLI not found. Attempting to install via npm if available..."
    try {
        & npm --version > $null 2>&1
        if ($?) {
            Write-Output "npm found. Installing supabase CLI globally (may require admin privileges)..."
            npm install -g supabase
            Write-Output "Installed supabase CLI."
        } else {
            Write-Output "npm not found. Install supabase CLI manually: https://supabase.com/docs/guides/cli"
            Exit 1
        }
    } catch {
        Write-Output "Failed to install supabase CLI automatically. Please install it and re-run this script."
        Exit 1
    }
}

# 2) Ask for project secrets (reads from environment if set)
$envServiceKey = [Environment]::GetEnvironmentVariable('SUPABASE_SERVICE_ROLE_KEY')
$envUrl = [Environment]::GetEnvironmentVariable('SUPABASE_URL')
$envOtpSalt = [Environment]::GetEnvironmentVariable('OTP_SALT')
$envResend = [Environment]::GetEnvironmentVariable('RESEND_API_KEY')

Write-Output "\n-- Supabase secrets --"
if ($envUrl) { Write-Output "SUPABASE_URL found in environment." } else { $envUrl = Read-Host "Enter SUPABASE_URL (https://<project>.supabase.co)" }
if ($envServiceKey) { Write-Output "SUPABASE_SERVICE_ROLE_KEY found in environment (hidden)." } else { $envServiceKey = Read-Host -AsSecureString "Enter SUPABASE_SERVICE_ROLE_KEY (will be stored as secret)"; $envServiceKey = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($envServiceKey)) }
if (-not $envOtpSalt) { $envOtpSalt = Read-Host "Enter OTP_SALT (or leave blank to set later)" }
if (-not $envResend) { $envResend = Read-Host "Enter RESEND_API_KEY (optional, leave blank to skip)" }

Write-Output "\nSetting Supabase function secrets (uses 'supabase secrets set')..."
# set secrets if provided
if ($envServiceKey) { supabase secrets set SUPABASE_SERVICE_ROLE_KEY="$envServiceKey" }
if ($envUrl) { supabase secrets set SUPABASE_URL="$envUrl" }
if ($envOtpSalt) { supabase secrets set OTP_SALT="$envOtpSalt" }
if ($envResend) { supabase secrets set RESEND_API_KEY="$envResend" }

Write-Output "\n-- Deploying functions --"
Push-Location "$(Resolve-Path ..\)" | Out-Null
# Ensure we are in repo root (script resides in supabase/)
$root = Get-Location
Write-Output "Repository root: $root"

# Deploy functions present in the repo
$functions = @('send-otp','verify-otp','complete-signup')
foreach ($f in $functions) {
    Write-Output "Deploying function: $f"
    supabase functions deploy $f
    if ($LASTEXITCODE -ne 0) {
        Write-Output "Deployment failed for $f (exit code $LASTEXITCODE). Check supabase CLI output above. Aborting."
        Exit 1
    }
}

Write-Output "\nDeploy complete.\n"

Write-Output "-- Quick test commands --"
Write-Output "1) Invoke send-otp (this will enqueue/send an OTP and may log the OTP in function logs for dev):"
Write-Output "   supabase functions invoke send-otp --data '{ \"email\": \"test@example.com\" }'"
Write-Output "2) Tail logs for send-otp (look for logged OTP in development fallback):"
Write-Output "   supabase functions logs send-otp --follow"
Write-Output "3) Once you have the OTP value, call complete-signup with otp + password + profile data (example):"
Write-Output "   supabase functions invoke complete-signup --data '{ \"email\": \"test@example.com\", \"password\": \"TestPass123!\", \"otp\": \"123456\", \"first_name\": \"F\", \"last_name\": \"L\" }'"
Write-Output "4) Tail complete-signup logs if needed:"
Write-Output "   supabase functions logs complete-signup --follow"

Write-Output "\nIf any deploy fails, open the Supabase Dashboard Edge Functions page to inspect the function source and secrets."

Pop-Location | Out-Null
Write-Output "Done."
