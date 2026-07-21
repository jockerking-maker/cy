import urllib.request, urllib.error, ssl, re, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

search_url = 'https://www.baidu.com/s?wd=zhuanlan.zhihu.com%2Fp%2F4130861066'
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

# Find the relevant section - look for "手把手教你封装" 
found = False
buffer = []
for i, line in enumerate(lines):
    line_stripped = line.strip()
    if '手把手教你封装' in line_stripped:
        found = True
    
    if found:
        buffer.append(line_stripped)
        if len(buffer) > 200:  # Get about 200 lines of content
            break

# Also search for lines with relevant content around the snippet
relevant_lines = []
capture = False
for i, line in enumerate(lines):
    ls = line.strip()
    if '手把手教你封装' in ls or '系统优化' in ls:
        capture = True
    
    if capture:
        relevant_lines.append(ls)
        if len(relevant_lines) >= 300:
            break

print('\n'.join(relevant_lines))
