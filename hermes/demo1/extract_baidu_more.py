import urllib.request, urllib.error, ssl, re, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Try to get more from Baidu - maybe there's a longer description
search_url = 'https://www.baidu.com/s?wd=%E6%89%8B%E6%8A%8A%E6%89%8B%E6%95%99%E4%BD%A0%E5%B0%81%E8%A3%85%E5%B1%9E%E4%BA%8E%E8%87%AA%E5%B7%B1%E7%9A%84Windows+ISO%E9%95%9C%E5%83%8F+%E8%AE%A9%E9%87%8D%E8%A3%85%E5%8F%98%E5%BE%97%E8%B6%85%E7%AE%80%E5%8D%95'
req = urllib.request.Request(search_url, data=None, headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'zh-CN,zh;q=0.9',
})
resp = urllib.request.urlopen(req, timeout=10, context=ctx)
text = resp.read().decode('utf-8', errors='replace')

# Remove scripts and styles
cleaned = re.sub(r'<script[^>]*>.*?</script>', '', text, flags=re.DOTALL)
cleaned = re.sub(r'<style[^>]*>.*?</style>', '', cleaned, flags=re.DOTALL)
cleaned = re.sub(r'<[^>]+>', '\n', cleaned)
cleaned = re.sub(r'\n{3,}', '\n\n', cleaned)

lines = cleaned.split('\n')

# Find all meaningful lines (more than 20 chars)
meaningful = []
for i, line in enumerate(lines):
    ls = line.strip()
    if len(ls) > 20:
        meaningful.append((i, ls))

for idx, (line_num, line) in enumerate(meaningful):
    print(f'[{line_num}] {line[:300]}')
