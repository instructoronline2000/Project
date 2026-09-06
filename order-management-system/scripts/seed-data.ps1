<#
.SYNOPSIS
  Seeds the order-management-system stack with demo customers, products and
  orders through the API Gateway (JWT-protected), so the H2 in-memory
  databases have realistic data ready for Postman / manual integration testing.

.DESCRIPTION
  Re-runnable: since every service uses an in-memory H2 database, all data is
  lost on `docker compose down` / restart. Run this script again afterward.
  Requires the full stack to already be up (see "how to run the application.md").

.EXAMPLE
  pwsh -File scripts/seed-data.ps1
#>

$ErrorActionPreference = "Stop"
$gateway = "http://localhost:8080"

Write-Host "== Logging in as admin ==" -ForegroundColor Cyan
$login = Invoke-RestMethod -Uri "$gateway/api/auth/login" -Method Post -ContentType "application/json" `
    -Body (@{ username = "admin"; password = "admin123" } | ConvertTo-Json)
$headers = @{ Authorization = "Bearer $($login.token)" }
Write-Host "Token acquired."

Write-Host "`n== Creating customers ==" -ForegroundColor Cyan
$customerDefs = @(
    @{ name = "John Doe";      email = "john.doe@example.com";      phone = "555-0101"; address = "123 Main St, Springfield" }
    @{ name = "Jane Smith";    email = "jane.smith@example.com";    phone = "555-0102"; address = "456 Oak Ave, Metropolis" }
    @{ name = "Carlos Diaz";   email = "carlos.diaz@example.com";   phone = "555-0103"; address = "789 Pine Rd, Gotham" }
    @{ name = "Aisha Khan";    email = "aisha.khan@example.com";    phone = "555-0104"; address = "12 Elm St, Star City" }
    @{ name = "Wei Chen";      email = "wei.chen@example.com";      phone = "555-0105"; address = "34 Birch Blvd, Central City" }
)
$customers = foreach ($c in $customerDefs) {
    $resp = Invoke-RestMethod -Uri "$gateway/api/customers" -Method Post -Headers $headers -ContentType "application/json" -Body ($c | ConvertTo-Json)
    Write-Host ("  customer #{0}: {1} <{2}>" -f $resp.id, $resp.name, $resp.email)
    $resp
}

Write-Host "`n== Creating products ==" -ForegroundColor Cyan
$productDefs = @(
    @{ name = "Wireless Mouse";              description = "Ergonomic wireless mouse";              price = 19.99;   stockQuantity = 200 }
    @{ name = "Mechanical Keyboard";         description = "RGB backlit mechanical keyboard";        price = 89.99;   stockQuantity = 150 }
    @{ name = "27-inch 4K Monitor";          description = "UHD IPS monitor";                        price = 349.99;  stockQuantity = 60 }
    @{ name = "USB-C Docking Station";       description = "11-in-1 docking station";                price = 129.50;  stockQuantity = 100 }
    @{ name = "Noise Cancelling Headphones"; description = "Over-ear ANC headphones";                price = 249.00;  stockQuantity = 80 }
    @{ name = "Laptop Stand";                description = "Aluminum adjustable laptop stand";       price = 45.00;   stockQuantity = 120 }
    @{ name = "Premium Ultrabook";           description = "High-end business laptop";               price = 1499.00; stockQuantity = 15 }
    @{ name = "1TB External SSD";            description = "Portable USB 3.2 SSD";                   price = 109.99;  stockQuantity = 90 }
    @{ name = "Low Stock Webcam";            description = "1080p webcam (kept low for stock tests)"; price = 39.99;  stockQuantity = 2 }
)
$products = foreach ($p in $productDefs) {
    $resp = Invoke-RestMethod -Uri "$gateway/api/products" -Method Post -Headers $headers -ContentType "application/json" -Body ($p | ConvertTo-Json)
    Write-Host ("  product #{0}: {1} (`${2}, stock={3})" -f $resp.id, $resp.name, $resp.price, $resp.stockQuantity)
    $resp
}

Write-Host "`n== Creating orders (drives the payment/notification saga) ==" -ForegroundColor Cyan
$orderDefs = @(
    @{ label = "Approved (under $1000 threshold)";   customerId = $customers[0].id; items = @(@{ productId = $products[0].id; quantity = 2 }, @{ productId = $products[1].id; quantity = 1 }) }
    @{ label = "Declined (over $1000 threshold)";     customerId = $customers[1].id; items = @(@{ productId = $products[6].id; quantity = 1 }) }
    @{ label = "Approved (multi-item)";               customerId = $customers[2].id; items = @(@{ productId = $products[2].id; quantity = 1 }, @{ productId = $products[4].id; quantity = 1 }) }
    @{ label = "Approved (small order)";               customerId = $customers[3].id; items = @(@{ productId = $products[5].id; quantity = 1 }) }
)
$orders = foreach ($o in $orderDefs) {
    $body = @{ customerId = $o.customerId; items = $o.items } | ConvertTo-Json -Depth 5
    $resp = Invoke-RestMethod -Uri "$gateway/api/orders" -Method Post -Headers $headers -ContentType "application/json" -Body $body
    Write-Host ("  order #{0}: customer={1} total={2} status={3}  [{4}]" -f $resp.id, $resp.customerId, $resp.totalAmount, $resp.status, $o.label)
    $resp
}

Write-Host "`n== Waiting for Kafka saga (payment + notification) to settle ==" -ForegroundColor Cyan
Start-Sleep -Seconds 5

Write-Host "`n== Payments ==" -ForegroundColor Cyan
$payments = Invoke-RestMethod -Uri "$gateway/api/payments" -Method Get -Headers $headers
$payments | ForEach-Object { Write-Host ("  payment #{0}: order={1} amount={2} status={3} reason={4}" -f $_.id, $_.orderId, $_.amount, $_.status, $_.declineReason) }

Write-Host "`n== Notifications (direct port, not routed through gateway) ==" -ForegroundColor Cyan
$notifications = Invoke-RestMethod -Uri "http://localhost:8085/api/notifications" -Method Get
$notifications | ForEach-Object { Write-Host ("  [{0}] order={1} channel={2} - {3}" -f $_.sentAt, $_.orderId, $_.channel, $_.message) }

Write-Host "`nSeed data loaded. IDs above are the ones referenced in docs/test manual.md." -ForegroundColor Green
