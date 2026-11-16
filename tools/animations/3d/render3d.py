"""Renders the 3D person on the 3D IM-309: perspective camera, soft lighting, ambient occlusion and
a floor shadow, drawn with a painter's sort of small triangles (supersampled, then scaled down)."""
import math
import numpy as np
import cv2
import body3d as B
import machine3d as M

W, H, SS = 640, 480, 2


def v(*a): return np.array(a, float)
def norm(a): return a / np.linalg.norm(a)


class Camera:
    def __init__(self, target, az, el, dist, fov=28):
        self.t = v(*target)
        a, e = math.radians(az), math.radians(el)
        self.pos = self.t + dist * v(math.cos(e) * math.sin(a), math.sin(e), math.cos(e) * math.cos(a))
        f = norm(self.t - self.pos); r = norm(np.cross(f, v(0, 1, 0))); u = np.cross(r, f)
        self.R = np.stack([r, u, -f]); self.f = (H * SS / 2) / math.tan(math.radians(fov / 2))

    def project(self, P):
        q = (P - self.pos) @ self.R.T
        z = -q[:, 2]
        x = W * SS / 2 + self.f * q[:, 0] / z
        y = H * SS / 2 - self.f * q[:, 1] / z
        return np.stack([x, y], 1), z


KEY = norm(v(-0.45, 0.85, 0.55)); FILL = norm(v(0.6, 0.3, -0.4)); RIM = norm(v(0.7, 0.4, -0.7))


def shade(C, N, view_dir, ao=None, spec=0.0):
    lam = np.clip(N @ KEY, 0, 1); fill = np.clip(N @ FILL, 0, 1) * 0.25
    rim = np.clip(N @ RIM, 0, 1) ** 3 * 0.25
    amb = 0.38 if ao is None else 0.38 * ao
    k = amb + 0.62 * lam * (1 if ao is None else (0.55 + 0.45 * ao)) + fill
    out = C * k[:, None] + 255 * rim[:, None] * 0.6
    if spec > 0:
        h = norm(KEY + view_dir)
        out += 255 * spec * np.clip(N @ h, 0, 1)[:, None] ** 24
    return np.clip(out, 0, 255)


def render(J, mus, state, cam, path=None):
    img = np.zeros((H * SS, W * SS, 3), np.float32)
    # backdrop: light studio grey, floor a little darker, soft horizon
    yy = np.linspace(0, 1, H * SS)[:, None]
    img[:] = (np.clip(245 - 25 * yy, 0, 255))[..., None] * np.ones(3)
    # body mesh
    BV, BF, BN = B.mesh(J)
    BC = B.colours(BV, BN, J, mus)
    d_out, _, _ = B.body_sdf(BV + BN * 3.0, J)
    ao = np.clip(d_out / 3.0, 0, 1) ** 0.6
    (MV, MF, MC), _ = M.machine(state)
    # floor shadow: everything squashed onto the floor along the light
    shadow = np.zeros((H * SS, W * SS), np.uint8)
    for V, F in ((BV, BF), (MV, MF)):
        P = V - KEY[None, :] * (V[:, 1:2] / KEY[1])
        P[:, 1] = 0.05
        S, _ = cam.project(P)
        cv2.fillPoly(shadow, [s.astype(np.int32) for s in S[F]], 255)
    shadow = cv2.GaussianBlur(shadow, (0, 0), 7 * SS).astype(np.float32) / 255
    img *= (1 - 0.35 * shadow)[..., None]
    # triangles of both, sorted far to near
    view = norm(cam.pos - cam.t)
    tri_n_b = BN[BF].mean(1); tri_n_b /= np.linalg.norm(tri_n_b, axis=1, keepdims=True)
    col_b = shade(BC[BF].mean(1), tri_n_b, view, ao[BF].mean(1), spec=0.08)
    e1 = MV[MF[:, 1]] - MV[MF[:, 0]]; e2 = MV[MF[:, 2]] - MV[MF[:, 0]]
    tn = np.cross(e1, e2); tn /= np.maximum(np.linalg.norm(tn, axis=1, keepdims=True), 1e-9)
    cen = MV[MF].mean(1)
    tn = np.where(((cam.pos - cen) * tn).sum(1, keepdims=True) < 0, -tn, tn)
    col_m = shade(MC, tn, view, spec=0.15)
    V = np.concatenate([BV, MV]); F = np.concatenate([BF, MF + len(BV)]); C = np.concatenate([col_b, col_m])
    S, z = cam.project(V)
    depth = z[F].mean(1)
    order = np.argsort(-depth)
    pts = (S[F] * 4).astype(np.int32)
    for i in order:
        c = C[i]
        cv2.fillConvexPoly(img, pts[i], (float(c[0]), float(c[1]), float(c[2])), lineType=cv2.LINE_AA, shift=2)
    out = cv2.resize(img, (W, H), interpolation=cv2.INTER_AREA)
    out = np.clip(out, 0, 255).astype(np.uint8)
    if path: cv2.imwrite(path, cv2.cvtColor(out, cv2.COLOR_RGB2BGR))
    return out
