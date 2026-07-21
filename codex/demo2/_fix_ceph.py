import re
path = " F:\\cy\\codex\\demo2\\ceph.md\
with open(path, \r\, encoding=\utf-8\) as f:
 content = f.read()

content = content.replace(\systemctl enable --now chronyd\, \systemctl enable --now chrony\)
content = content.replace(\ceph orch host add ceph2\, \ceph orch host add ceph2 --labels mon\)
content = content.replace(\ceph orch host add ceph3\, \ceph orch host add ceph3 --labels mon\)

with open(path, \w\, encoding=\utf-8\) as f:
 f.write(content)
print(\Done\)
