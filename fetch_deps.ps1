param([string[]]$Artifacts)

$cacheBase = "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1"
$mavenBase = "https://dl.google.com/dl/android/maven2"

function Download-Artifact {
    param([string]$Group, [string]$ArtifactId, [string]$Version)
    
    $groupPath = $Group -replace '\.', '/'
    $baseUrl = "$mavenBase/$groupPath/$ArtifactId/$Version/$ArtifactId-$Version"
    $cacheDir = "$cacheBase\$Group\$ArtifactId\$Version"
    
    # Determine extensions to try
    $extensions = @("pom", "module", "aar", "jar")
    
    foreach ($ext in $extensions) {
        $url = "$baseUrl.$ext"
        $filename = "$ArtifactId-$Version.$ext"
        $phonePath = "/sdcard/Download/$filename"
        
        # Download via ADB
        $result = adb shell "curl -s -o '$phonePath' '$url' -w '%{http_code}'" 2>&1
        $httpCode = ($result | Select-String -Pattern '\d{3}$').Matches.Value
        
        if ($httpCode -eq "200") {
            # Pull to local
            $localPath = "$PSScriptRoot\$filename"
            adb pull $phonePath $localPath 2>&1 | Out-Null
            
            if (Test-Path $localPath) {
                $sha1 = (Get-FileHash -Path $localPath -Algorithm SHA1).Hash.ToLower()
                $targetDir = "$cacheDir\$sha1"
                New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
                Move-Item $localPath "$targetDir\$filename" -Force
                Write-Host "  OK: $filename ($sha1)" -ForegroundColor Green
            }
        } else {
            # Clean up failed download
            adb shell "rm -f '$phonePath'" 2>&1 | Out-Null
        }
    }
}

foreach ($artifact in $Artifacts) {
    $parts = $artifact -split ':'
    if ($parts.Count -eq 3) {
        Write-Host "Fetching $artifact..." -ForegroundColor Cyan
        Download-Artifact -Group $parts[0] -ArtifactId $parts[1] -Version $parts[2]
    }
}

Write-Host "`nDone fetching artifacts." -ForegroundColor Yellow
