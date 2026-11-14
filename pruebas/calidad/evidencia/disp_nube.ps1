$urls = @('https://serviciosgeneralespo.duckdns.org/','https://serviciosgeneralespo.duckdns.org/servicios','https://serviciosgeneralespo.duckdns.org/contacto','https://serviciosgeneralespo.duckdns.org/login')
$out = 'C:\Users\USER\Desktop\INTEGRADOR_2\disp_nube.csv'
'hora,url,codigo,ms' | Out-File $out -Encoding ascii
for ($i = 1; $i -le 20; $i++) {
  foreach ($u in $urls) {
    $sw = [Diagnostics.Stopwatch]::StartNew()
    $code = 0
    try { $r = Invoke-WebRequest -Uri $u -UseBasicParsing -TimeoutSec 15; $code = [int]$r.StatusCode }
    catch { if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode } else { $code = 0 } }
    $sw.Stop()
    ((Get-Date).ToString('HH:mm:ss') + ',' + $u + ',' + $code + ',' + $sw.ElapsedMilliseconds) | Out-File $out -Append -Encoding ascii
  }
  if ($i -lt 20) { Start-Sleep -Seconds 30 }
}
'FIN' | Out-File $out -Append -Encoding ascii
