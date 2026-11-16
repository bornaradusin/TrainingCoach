"""Puts the rendered frames (out/<id>/NN.png) into the app as WebP: app/src/main/assets/anim3d/<id>/NN.webp."""
import os, glob, cv2
here = os.path.dirname(os.path.abspath(__file__))
dest = os.path.join(here, '../../../app/src/main/assets/anim3d')
total = 0
for d in sorted(glob.glob(os.path.join(here, 'out', '*'))):
    eid = os.path.basename(d); os.makedirs(os.path.join(dest, eid), exist_ok=True)
    for f in sorted(glob.glob(os.path.join(d, '*.png'))):
        out = os.path.join(dest, eid, os.path.basename(f)[:-4] + '.webp')
        cv2.imwrite(out, cv2.imread(f), [cv2.IMWRITE_WEBP_QUALITY, 82])
        total += os.path.getsize(out)
print('%.1f MB' % (total / 1e6))
