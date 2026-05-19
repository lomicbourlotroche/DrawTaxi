# Script pour corriger tous les imports manquants

# 1. SmsProcessor.kt - QuoteResponseHandler
$file = 'C:\Users\lomic\Desktop\DrawTaxi\app\src\main\java\com\drawtaxi\app\logic\sms\SmsProcessor.kt'
$content = Get-Content $file -Raw
if ($content -notmatch 'import com.drawtaxi.app.logic.pricing.QuoteResponseHandler') {
    $content = $content -replace 'import com.drawtaxi.app.data.RideRequest', 'import com.drawtaxi.app.data.RideRequest`nimport com.drawtaxi.app.logic.pricing.QuoteResponseHandler'
    Set-Content $file -Value $content -NoNewline
    Write-Host "Fixed: SmsProcessor.kt"
}

# 2. SmsScanWorker.kt - SmsScanner, NotificationHelper
$file = 'C:\Users\lomic\Desktop\DrawTaxi\app\src\main\java\com\drawtaxi\app\service\worker\SmsScanWorker.kt'
$content = Get-Content $file -Raw
if ($content -notmatch 'import com.drawtaxi.app.logic.sms.SmsScanner') {
    $content = $content -replace 'import com.drawtaxi.app.data.TaxiRepository', 'import com.drawtaxi.app.data.TaxiRepository`nimport com.drawtaxi.app.logic.messaging.NotificationHelper`nimport com.drawtaxi.app.logic.sms.SmsScanner'
    Set-Content $file -Value $content -NoNewline
    Write-Host "Fixed: SmsScanWorker.kt"
}

# 3. StatsReportScheduler.kt - NotificationHelper
$file = 'C:\Users\lomic\Desktop\DrawTaxi\app\src\main\java\com\drawtaxi\app\service\worker\StatsReportScheduler.kt'
$content = Get-Content $file -Raw
if ($content -notmatch 'import com.drawtaxi.app.logic.messaging.NotificationHelper') {
    $content = $content -replace 'import com.drawtaxi.app.data.TaxiRepository', 'import com.drawtaxi.app.data.TaxiRepository`nimport com.drawtaxi.app.logic.messaging.NotificationHelper'
    Set-Content $file -Value $content -NoNewline
    Write-Host "Fixed: StatsReportScheduler.kt"
}

# 4. SmsForegroundService.kt - SmsProcessor, NotificationHelper
$file = 'C:\Users\lomic\Desktop\DrawTaxi\app\src\main\java\com\drawtaxi\app\service\foreground\SmsForegroundService.kt'
$content = Get-Content $file -Raw
if ($content -notmatch 'import com.drawtaxi.app.logic.sms.SmsProcessor') {
    $content = $content -replace 'import com.drawtaxi.app.data.TaxiRepository', 'import com.drawtaxi.app.data.TaxiRepository`nimport com.drawtaxi.app.logic.messaging.NotificationHelper`nimport com.drawtaxi.app.logic.sms.SmsProcessor'
    Set-Content $file -Value $content -NoNewline
    Write-Host "Fixed: SmsForegroundService.kt"
}

# 5. LocationTrackingService.kt - NotificationHelper
$file = 'C:\Users\lomic\Desktop\DrawTaxi\app\src\main\java\com\drawtaxi\app\service\tracking\LocationTrackingService.kt'
$content = Get-Content $file -Raw
if ($content -notmatch 'import com.drawtaxi.app.logic.messaging.NotificationHelper') {
    $content = $content -replace 'import android.util.Log', 'import android.util.Log`nimport com.drawtaxi.app.logic.messaging.NotificationHelper'
    Set-Content $file -Value $content -NoNewline
    Write-Host "Fixed: LocationTrackingService.kt"
}

# 6. OvhImapService.kt - AiSmsParser, NotificationHelper
$file = 'C:\Users\lomic\Desktop\DrawTaxi\app\src\main\java\com\drawtaxi\app\service\foreground\OvhImapService.kt'
$content = Get-Content $file -Raw
if ($content -notmatch 'import com.drawtaxi.app.logic.sms.AiSmsParser') {
    $content = $content -replace 'import com.drawtaxi.app.data.AppSettings', 'import com.drawtaxi.app.data.AppSettings`nimport com.drawtaxi.app.logic.messaging.NotificationHelper`nimport com.drawtaxi.app.logic.sms.AiSmsParser'
    Set-Content $file -Value $content -NoNewline
    Write-Host "Fixed: OvhImapService.kt"
}

# 7. RouteToClientMap.kt - FetchRoute
$file = 'C:\Users\lomic\Desktop\DrawTaxi\app\src\main\java\com\drawtaxi\app\ui\components\RouteToClientMap.kt'
$content = Get-Content $file -Raw
if ($content -notmatch 'import com.drawtaxi.app.logic.routing.FetchRoute') {
    $content = $content -replace 'import com.drawtaxi.app.logic.geocoding.GeocodingService', 'import com.drawtaxi.app.logic.geocoding.GeocodingService`nimport com.drawtaxi.app.logic.routing.FetchRoute'
    Set-Content $file -Value $content -NoNewline
    Write-Host "Fixed: RouteToClientMap.kt"
}

# 8. TaxiViewModel.kt - ParseSms
$file = 'C:\Users\lomic\Desktop\DrawTaxi\app\src\main\java\com\drawtaxi\app\ui\TaxiViewModel.kt'
$content = Get-Content $file -Raw
if ($content -notmatch 'import com.drawtaxi.app.logic.sms.ParseSms') {
    $content = $content -replace 'import com.drawtaxi.app.data.Quote', 'import com.drawtaxi.app.data.Quote`nimport com.drawtaxi.app.logic.sms.ParseSms'
    Set-Content $file -Value $content -NoNewline
    Write-Host "Fixed: TaxiViewModel.kt"
}

# 9. DashboardScreen.kt - DashboardPeriod, DailyBreakdown, PeriodStats
$file = 'C:\Users\lomic\Desktop\DrawTaxi\app\src\main\java\com\drawtaxi\app\ui\screens\dashboard\DashboardScreen.kt'
$content = Get-Content $file -Raw
if ($content -notmatch 'import com.drawtaxi.app.logic.pricing.DashboardPeriod') {
    $content = $content -replace 'import com.drawtaxi.app.data.RideStatus', 'import com.drawtaxi.app.data.RideStatus`nimport com.drawtaxi.app.logic.pricing.DashboardPeriod`nimport com.drawtaxi.app.logic.pricing.DailyBreakdown`nimport com.drawtaxi.app.logic.pricing.PeriodStats'
    Set-Content $file -Value $content -NoNewline
    Write-Host "Fixed: DashboardScreen.kt"
}

Write-Host "All files fixed!"
