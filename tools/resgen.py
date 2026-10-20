#!/usr/bin/env python3
"""
Kiểm tra resource Android không cần SDK:
  1. Thu thập resource (values/*, drawable, layout, menu, anim, animator, color, mipmap...).
  2. Kiểm tra mọi tham chiếu @type/name trong XML (và style parent) đều tồn tại.
  3. Sinh R.java và các lớp ViewBinding (giống AGP) để javac kiểm tra code Java.
"""
import os, re, sys, glob
from lxml import etree

RES = sys.argv[1]
OUT = sys.argv[2]
PKG = "com.example.gymmanagement"
A = "{http://schemas.android.com/apk/res/android}"

errors = []
res = {}  # type -> set(names)
def add(t, n): res.setdefault(t, set()).add(n)

# ---- 1. Collect ----
for d in sorted(os.listdir(RES)):
    path = os.path.join(RES, d)
    base = d.split("-")[0]
    for f in sorted(os.listdir(path)):
        fp = os.path.join(path, f)
        name, ext = os.path.splitext(f)
        if base == "values":
            t = etree.parse(fp)
            for el in t.getroot():
                if not isinstance(el.tag, str):
                    continue
                tag = el.tag
                n = el.get("name")
                if tag == "item":
                    tag = el.get("type")
                if tag in ("string", "color", "dimen", "bool", "integer", "style", "plurals", "attr", "array",
                           "string-array", "integer-array"):
                    tt = {"string-array": "array", "integer-array": "array"}.get(tag, tag)
                    add(tt, n)
                elif tag == "declare-styleable":
                    add("styleable", n)
        else:
            if ext not in (".xml", ".png", ".jpg", ".webp", ".9"):
                errors.append(f"Unexpected file {fp}")
            if name.endswith(".9"):
                name = name[:-2]
            add(base, name)
            if ext == ".xml" and base in ("layout", "menu", "drawable", "xml"):
                txt = open(fp, encoding="utf-8").read()
                for m in re.finditer(r'@\+id/([A-Za-z0-9_]+)', txt):
                    add("id", m.group(1))
            if ext == ".xml" and base in ("drawable", "animator", "anim"):
                # android:name targets in animated-vector are not R.id
                pass

# ---- 2. Check references ----
LIB_STYLE_PREFIX = ("Theme.Material3", "Widget.Material3", "TextAppearance.Material3", "ThemeOverlay.Material3",
                    "Theme.SplashScreen", "Widget.MaterialComponents", "Theme.AppCompat", "Widget.AppCompat",
                    "ShapeAppearance.Material3")
LIB_REFS = {("string", "appbar_scrolling_view_behavior"), ("string", "bottom_sheet_behavior")}
ref_re = re.compile(r'@(?!\+)(?:(android|[a-z.]+):)?([a-z]+)/([A-Za-z0-9_.]+)')
for d in sorted(os.listdir(RES)):
    for f in sorted(os.listdir(os.path.join(RES, d))):
        fp = os.path.join(RES, d, f)
        if not f.endswith(".xml"):
            continue
        txt = open(fp, encoding="utf-8").read()
        for m in ref_re.finditer(txt):
            ns, t, n = m.group(1), m.group(2), m.group(3)
            if ns == "android":
                continue
            if t in ("id",) and n in res.get("id", set()):
                continue
            if t == "style":
                n2 = n
                if n2 in res.get("style", set()) or n2.startswith(LIB_STYLE_PREFIX):
                    continue
                errors.append(f"{fp}: missing style {n}")
                continue
            if (t, n) in LIB_REFS:
                continue
            if n not in res.get(t, set()):
                errors.append(f"{fp}: missing @{t}/{n}")
        if d.startswith("values"):
            t = etree.parse(fp)
            for el in t.getroot():
                if isinstance(el.tag, str) and el.tag == "style":
                    p = el.get("parent")
                    n = el.get("name")
                    if p is None:
                        # implicit parent by dot notation
                        if "." in n:
                            ip = n.rsplit(".", 1)[0]
                            if ip not in res["style"] and not ip.startswith(LIB_STYLE_PREFIX) and ip not in ("TextAppearance", "Widget", "Theme", "ThemeOverlay", "ShapeAppearance"):
                                errors.append(f"{fp}: style {n} implicit parent {ip} missing")
                        continue
                    if p == "":
                        continue
                    p = p.replace("@style/", "").replace("@android:style/", "android:")
                    if p.startswith("android:"):
                        continue
                    if p not in res["style"] and not p.startswith(LIB_STYLE_PREFIX):
                        errors.append(f"{fp}: style {n} parent {p} missing")

# ---- 3. Generate R.java ----
def jname(n): return n.replace(".", "_")
os.makedirs(os.path.join(OUT, PKG.replace(".", "/")), exist_ok=True)
lines = [f"package {PKG};", "public final class R {"]
counter = [0x7f000000]
for t in sorted(res):
    if t == "styleable":
        continue
    lines.append(f"  public static final class {t} {{")
    for n in sorted(res[t]):
        counter[0] += 1
        lines.append(f"    public static final int {jname(n)} = {counter[0]};")
    lines.append("  }")
lines.append("}")
open(os.path.join(OUT, PKG.replace(".", "/"), "R.java"), "w").write("\n".join(lines))

# ---- 4. ViewBinding ----
def camel(s):
    parts = s.split("_")
    return parts[0] + "".join(p[:1].upper() + p[1:] for p in parts[1:])
def pascal(s):
    return "".join(p[:1].upper() + p[1:] for p in s.split("_"))
SIMPLE = {"View": "android.view.View", "ViewStub": "android.view.ViewStub", "SurfaceView": "android.view.SurfaceView",
          "TextureView": "android.view.TextureView"}
def jtype(tag):
    if "." in tag:
        return tag
    if tag in SIMPLE:
        return SIMPLE[tag]
    if tag == "fragment":
        return "android.view.View"
    return "android.widget." + tag

bdir = os.path.join(OUT, PKG.replace(".", "/"), "databinding")
os.makedirs(bdir, exist_ok=True)
layout_dir = os.path.join(RES, "layout")
for f in sorted(os.listdir(layout_dir)):
    name = f[:-4]
    t = etree.parse(os.path.join(layout_dir, f))
    root = t.getroot()
    cls = pascal(name) + "Binding"
    is_merge = root.tag == "merge"
    root_type = "android.view.View" if is_merge else jtype(root.tag)
    fields = []
    seen = set()
    for el in root.iter():
        if not isinstance(el.tag, str):
            continue
        idv = el.get(A + "id")
        if not idv or not idv.startswith("@+id/") and not idv.startswith("@id/"):
            continue
        idn = idv.split("/", 1)[1]
        if idn in seen:
            errors.append(f"layout {f}: duplicate id {idn}")
            continue
        seen.add(idn)
        if el.tag == "include":
            inc = el.get("layout").split("/", 1)[1]
            ft = PKG + ".databinding." + pascal(inc) + "Binding"
        elif el.tag == "merge":
            continue
        else:
            ft = jtype(el.tag)
        fields.append((camel(idn), ft, idn, el is root))
    out = [f"package {PKG}.databinding;", "", "import android.view.LayoutInflater;", "import android.view.View;",
           "import android.view.ViewGroup;", "", f"public final class {cls} implements androidx.viewbinding.ViewBinding {{"]
    out.append(f"  private final {root_type} rootView;")
    for fn, ft, idn, isroot in fields:
        out.append(f"  public final {ft} {fn};")
    ctor_params = ", ".join([f"{root_type} rootView"] + [f"{ft} {fn}" for fn, ft, _, _ in fields])
    out.append(f"  private {cls}({ctor_params}) {{ this.rootView = rootView;" +
               "".join(f" this.{fn} = {fn};" for fn, _, _, _ in fields) + " }")
    out.append(f"  @Override public {root_type} getRoot() {{ return rootView; }}")
    if is_merge:
        out.append(f"  public static {cls} inflate(LayoutInflater inflater, ViewGroup parent) {{ return bind(parent); }}")
    else:
        out.append(f"  public static {cls} inflate(LayoutInflater inflater) {{ return inflate(inflater, null, false); }}")
        out.append(f"  public static {cls} inflate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent) {{ return bind(inflater.inflate(0, parent, attachToParent)); }}")
    out.append(f"  public static {cls} bind(View rootView) {{ throw new RuntimeException(); }}")
    out.append("}")
    open(os.path.join(bdir, cls + ".java"), "w").write("\n".join(out))

# ---- report ----
print(f"resource types: " + ", ".join(f"{k}={len(v)}" for k, v in sorted(res.items())))
if errors:
    print("ERRORS:")
    for e in errors:
        print("  " + e)
    sys.exit(1)
print("Resource check OK")
