import re
from html.parser import HTMLParser

class MLStripper(HTMLParser):
    def __init__(self):
        super().__init__()
        self.reset()
        self.strict = False
        self.convert_charrefs = True
        self.text = []
    def handle_data(self, d):
        self.text.append(d)
    def get_data(self):
        return ''.join(self.text)

def strip_tags(html):
    s = MLStripper()
    s.feed(html)
    return s.get_data()

with open("C:\\tmp\\wiki_gdp.html", "r", encoding="utf-8") as f:
    html = f.read()

# Find the 2025 table - look for the table that has "2025" in its caption or headers
tables = re.findall(r'<table[^>]*class="wikitable[^"]*sortable[^"]*"[^>]*>.*?</table>', html, re.DOTALL)
print(f"Found {len(tables)} wikitable sortable tables")

for i, t in enumerate(tables):
    if "2025" in t:
        print(f"\n=== Table {i} has 2025 ({len(t)} chars) ===")
        # Try to find rows - some tables use <tbody>
        # First check the structure
        has_tbody = '<tbody>' in t
        print(f"Has tbody: {has_tbody}")
        
        # Extract rows from tbody or directly
        if has_tbody:
            body = re.search(r'<tbody>(.*?)</tbody>', t, re.DOTALL)
            if body:
                t = body.group(1)
        
        rows = re.findall(r'<tr[^>]*>(.*?)</tr>', t, re.DOTALL)
        print(f"Rows: {len(rows)}")
        
        for j, row in enumerate(rows):
            cells = re.findall(r'<t[dh][^>]*>(.*?)</t[dh]>', row, re.DOTALL)
            clean = []
            for c in cells:
                c = strip_tags(c).strip()
                clean.append(c)
            if j < 35:
                print(f"  Row {j}: {clean}")
