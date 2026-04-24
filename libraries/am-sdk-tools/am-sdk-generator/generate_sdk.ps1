param(
    [Parameter(Mandatory = $true)][string]$InputSpec,
    [Parameter(Mandatory = $true)][string]$OutputDir,
    [string]$Language = "java", # "java" or "flutter"
    [string]$PackageName = "com.am.client",
    [string]$ArtifactId = "am-client",
    [string]$PubName = "am_client",
    [string]$PubVersion = "1.0.0"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Ensure Output Dir exists (and clean it?)
if (Test-Path $OutputDir) {
    Write-Host "Cleaning output directory: $OutputDir" -ForegroundColor Yellow
    Remove-Item -Recurse -Force $OutputDir
}
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

if ($Language -eq "java") {
    Write-Host "Generating Java SDK..." -ForegroundColor Cyan
    
    # Generate
    npx @openapitools/openapi-generator-cli generate `
        -i $InputSpec `
        -g java `
        -o $OutputDir `
        --additional-properties="groupId=com.am,artifactId=$ArtifactId,apiPackage=${PackageName}.api,modelPackage=${PackageName}.model,invokerPackage=${PackageName}.invoker,library=native,dateLibrary=java8"

    # Inject LoggingHttpClient
    $invokerDir = Join-Path $OutputDir "src\main\java\$(($PackageName -replace '\.','\'))\invoker"
    $loggingFile = Join-Path $invokerDir "LoggingHttpClient.java"
    
    $template = Get-Content (Join-Path $ScriptDir "resources\java\LoggingHttpClient.java.template") -Raw
    $content = $template -replace "\{\{PackageName\}\}", "${PackageName}.invoker"
    Set-Content -Path $loggingFile -Value $content

    # Modify ApiClient.java to use LoggingHttpClient
    $apiClientFile = Join-Path $invokerDir "ApiClient.java"
    if (Test-Path $apiClientFile) {
        $apiContent = Get-Content -Path $apiClientFile -Raw
        # Replace builder.build() with new LoggingHttpClient(builder.build()) inside getHttpClient()
        # Pattern match specifically for getHttpClient method return
        $apiContent = $apiContent -replace "return builder.build\(\);", "return new LoggingHttpClient(builder.build());"
        Set-Content -Path $apiClientFile -Value $apiContent
        Write-Host " injected LoggingHttpClient into ApiClient.java" -ForegroundColor Green
    }
    else {
        Write-Host "Warning: ApiClient.java not found at $apiClientFile" -ForegroundColor Red
    }

    # Inject slf4j dependency into pom.xml
    $pomFile = Join-Path $OutputDir "pom.xml"
    if (Test-Path $pomFile) {
        $pomContent = Get-Content -Path $pomFile -Raw
        if (-not ($pomContent -match "slf4j-api")) {
            $dep = @"
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.12</version>
        </dependency>
    </dependencies>
"@
            $pomContent = $pomContent -replace "</dependencies>", $dep
            Set-Content -Path $pomFile -Value $pomContent
            Write-Host " injected slf4j dependency into pom.xml" -ForegroundColor Green
        }
    }

}
elseif ($Language -eq "flutter") {
    Write-Host "Generating Flutter SDK..." -ForegroundColor Cyan

    # Generate
    npx @openapitools/openapi-generator-cli generate `
        -i $InputSpec `
        -g dart `
        -o $OutputDir `
        --additional-properties="pubName=$PubName,pubVersion=$PubVersion"

    $libDir = Join-Path $OutputDir "lib"
    
    # Check for api_client.dart location.
    $apiClientFile = Get-ChildItem -Path $libDir -Recurse -Filter "api_client.dart" | Select-Object -First 1
    
    if ($apiClientFile) {
        $apiClientDir = $apiClientFile.DirectoryName
        
        # Inject LoggingClient.dart content from template
        $template = Get-Content (Join-Path $ScriptDir "resources\flutter\LoggingClient.dart.template") -Raw
        
        # Modify ApiClient.dart to use LoggingClient
        $content = Get-Content -Path $apiClientFile.FullName -Raw
        
        # Append LoggingClient class definition at the end (simplest way to avoid file creation issues in part-of system)
        $content += "`n`n" + $template
        
        # Replace client instantiation with LoggingClient
        # The generated code usually has: var _client = Client();
        # We replace it with: Client _client = LoggingClient(Client());
        $content = $content -replace "var _client = Client\(\);", "Client _client = LoggingClient(Client());"
        
        Set-Content -Path $apiClientFile.FullName -Value $content
        Write-Host " injected LoggingClient into $($apiClientFile.Name)" -ForegroundColor Green
    }
    else {
        Write-Host "Warning: api_client.dart not found in $libDir" -ForegroundColor Red
    }
}
else {
    Write-Host "Unknown language: $Language" -ForegroundColor Red
    exit 1
}

Write-Host "SDK Generation Complete." -ForegroundColor Green
