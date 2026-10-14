param(
    [string]$Database = 'lauriel',
    [string]$AdminUser = 'root',
    [string]$HostName = 'localhost',
    [int]$Port = 3306
)

$ErrorActionPreference = 'Stop'
$mysqlCandidates = @(
    'C:\Program Files\MySQL\MySQL Server 9.7\bin\mysql.exe',
    'C:\Program Files\MySQL\MySQL Workbench 8.0 CE\mysql.exe'
)
$mysql = $mysqlCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $mysql) {
    $command = Get-Command mysql -ErrorAction SilentlyContinue
    if ($command) { $mysql = $command.Source }
}
if (-not $mysql) { throw 'mysql.exe was not found. Install MySQL Client or update its path in this script.' }

$securePassword = Read-Host "MySQL password for $AdminUser" -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $env:MYSQL_PWD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    $commonArguments = @(
        "--host=$HostName",
        "--port=$Port",
        "--user=$AdminUser",
        "--database=$Database",
        '--default-character-set=utf8mb4'
    )
    $stateQuery = "SELECT CONCAT((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$Database' AND TABLE_NAME='categories'), ':', (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='$Database' AND TABLE_NAME='products' AND COLUMN_NAME='category_id'), ':', (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='$Database' AND TABLE_NAME='orders' AND COLUMN_NAME='shipping_fee'), ':', (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$Database' AND TABLE_NAME='product_images'), ':', (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='$Database' AND TABLE_NAME='product_images' AND COLUMN_NAME='size_id'), ':', (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='$Database' AND TABLE_NAME='product_images' AND COLUMN_NAME='color_id'), ':', (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$Database' AND TABLE_NAME='customer_activity'), ':', (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$Database' AND TABLE_NAME='customer_cart_items'), ':', (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$Database' AND TABLE_NAME='promotion_variants'), ':', (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$Database' AND TABLE_NAME='order_promotions'));"
    $state = & $mysql @commonArguments --batch --skip-column-names --execute=$stateQuery
    if ($LASTEXITCODE -ne 0) { throw 'Could not connect to MySQL with the supplied administrator account.' }

    switch ($state) {
        '1:1:1:1:0:1:1:1:1:1' { $pendingMigrations = @() }
        '1:1:1:1:0:1:1:1:1:0' { $pendingMigrations = @('V9__add_order_promotions.sql') }
        '1:1:1:1:0:1:1:1:0:0' { $pendingMigrations = @('V8__add_promotion_variants.sql','V9__add_order_promotions.sql') }
        '1:1:1:1:0:1:0:0:0:0' { $pendingMigrations = @('V7__add_customer_engagement.sql','V8__add_promotion_variants.sql','V9__add_order_promotions.sql') }
        '1:1:1:1:1:0:0:0:0:0' { $pendingMigrations = @('V6__change_product_image_size_to_color.sql','V7__add_customer_engagement.sql','V8__add_promotion_variants.sql','V9__add_order_promotions.sql') }
        '1:1:1:1:0:0:0:0:0:0' { $pendingMigrations = @('V5__add_product_image_size.sql','V6__change_product_image_size_to_color.sql','V7__add_customer_engagement.sql','V8__add_promotion_variants.sql','V9__add_order_promotions.sql') }
        '0:0:0:0:0:0:0:0:0:0' { $pendingMigrations = @('V2__add_product_categories.sql','V3__add_shipping_fee.sql','V4__add_product_images.sql','V5__add_product_image_size.sql','V6__change_product_image_size_to_color.sql','V7__add_customer_engagement.sql','V8__add_promotion_variants.sql','V9__add_order_promotions.sql') }
        default { throw "Schema is partially migrated ($state). Inspect it manually before continuing." }
    }

    foreach ($name in $pendingMigrations) {
        $path = Join-Path $PSScriptRoot $name
        Write-Host "Applying $name..."
        Get-Content -Raw -Encoding UTF8 -LiteralPath $path | & $mysql @commonArguments
        if ($LASTEXITCODE -ne 0) { throw "Migration failed: $name" }
    }

    $superAdminRole = & $mysql @commonArguments --batch --skip-column-names --execute="SELECT COUNT(*) FROM roles WHERE role_name='SUPER_ADMIN';"
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect the SUPER_ADMIN role.' }
    if ($superAdminRole -eq '0') {
        $name = 'V10__add_super_admin_role.sql'
        Write-Host "Applying $name..."
        Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $PSScriptRoot $name) | & $mysql @commonArguments
        if ($LASTEXITCODE -ne 0) { throw "Migration failed: $name" }
    }

    $verified = & $mysql @commonArguments --batch --skip-column-names --execute=$stateQuery
    if ($LASTEXITCODE -ne 0 -or $verified -ne '1:1:1:1:0:1:1:1:1:1') {
        throw "Could not verify schema after migration: $verified"
    }
    $verifiedRole = & $mysql @commonArguments --batch --skip-column-names --execute="SELECT COUNT(*) FROM roles WHERE role_name='SUPER_ADMIN';"
    if ($LASTEXITCODE -ne 0 -or $verifiedRole -ne '1') { throw 'Could not verify V10 SUPER_ADMIN role.' }
    Write-Host 'V2-V10 are fully applied.' -ForegroundColor Green
}
finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
}
