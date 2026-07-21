$path = "F:\cy\codex\demo2\ceph.md"
$content = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)

$content = $content.Replace("systemctl enable --now chronyd", "systemctl enable --now chrony")
$content = $content.Replace("ceph orch host add ceph2", "ceph orch host add ceph2 --labels mon")
$content = $content.Replace("ceph orch host add ceph3", "ceph orch host add ceph3 --labels mon")

[System.IO.File]::WriteAllText($path, $content, [System.Text.Encoding]::UTF8)
Write-Host "Step 1-2 done"
