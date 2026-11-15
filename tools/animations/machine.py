"""The IM-309 to scale, in centimetres (x from the front, y up from the floor), measured from the
product photos. Shapes are drawn in order; 'l' is the layer: back (behind the person) or front."""

SIDE = dict(
    pulleys=dict(HP=[73, 186], LP=[8, 9], MP=[121, 138], BEAM=[176, 158]),
    pivots=dict(PRESS=[97, 152], LEG=[43, 60]),
    stack=dict(x=156, y=6, w=30, plates=12, h=3.2, gap=0.35, top=[171, 150]),
    rest=dict(LEG=[43, 18]),
    shapes=[
        dict(t="rect", x=-2, y=0, w=198, h=4, c="frame", l="back"),
        dict(t="rect", x=-4, y=0, w=12, h=6, c="frame", l="back"),
        dict(t="rect", x=180, y=0, w=16, h=6, c="frame", l="back"),
        dict(t="rect", x=152, y=4, w=3.5, h=148, c="frame", l="back"),        # stack uprights
        dict(t="rect", x=186.5, y=4, w=3.5, h=148, c="frame", l="back"),
        dict(t="line", a=[171, 8], b=[171, 150], w=1.2, c="chrome", l="back"),  # guide rod
        dict(t="line", a=[90, 4], b=[131, 172], w=6, c="frame", l="back"),      # main post
        dict(t="line", a=[60, 201], b=[190, 152], w=6, c="frame", l="back"),    # top beam
        dict(t="line", a=[97, 150], b=[97, 191], w=5, c="frame", l="back"),     # press arm hanger
        dict(t="line", a=[73, 196], b=[73, 190], w=2, c="frame", l="back"),
        dict(t="line", a=[35, 4], b=[35, 82], w=5, c="frame", l="back"),        # front post
        dict(t="line", a=[35, 42], b=[86, 42], w=4, c="frame", l="back"),       # seat beam
        dict(t="line", a=[70, 42], b=[80, 4], w=4, c="frame", l="back"),
        dict(t="line", a=[43, 60], b=[52, 42], w=3, c="frame", l="back"),
        dict(t="rrect", x=52, y=50, w=37, h=8, r=3, c="pad", l="back"),          # seat
        dict(t="line", a=[100, 58], b=[118, 133], w=10, c="pad", l="back"),     # backrest
        dict(t="line", a=[22, 75], b=[48, 91], w=8, c="pad", l="back"),         # preacher pad
        dict(t="circle", x=43, y=60, r=2.2, c="frame", l="back"),               # leg lever pivot
        dict(t="circle", x=73, y=186, r=4, c="pulley", l="back"),
        dict(t="circle", x=126, y=176, r=3.5, c="pulley", l="back"),
        dict(t="circle", x=176, y=158, r=3.5, c="pulley", l="back"),
        dict(t="circle", x=121, y=138, r=3, c="pulley", l="back"),
        dict(t="circle", x=8, y=9, r=3.5, c="pulley", l="back"),
        dict(t="line", a=[12, 7], b=[152, 7], w=0.8, c="chrome", l="back"),     # low cable to the back
        dict(t="circle", x=34, y=63, r=5, c="roller", l="front"),               # upper rollers
    ],
)

FRONT = dict(
    pulleys=dict(HP=[0, 194], LP=[0, 9], MP=[0, 138]),
    pivots=dict(PRESS_R=[9, 156], PRESS_L=[-9, 156]),
    stack=None,
    shapes=[
        dict(t="rect", x=-52, y=0, w=104, h=5, c="frame", l="back"),
        dict(t="rect", x=-16, y=5, w=32, h=147, c="stackbox", l="back"),        # stack behind
        dict(t="rect", x=-5, y=150, w=10, h=56, c="frame", l="back"),           # top beam end-on
        dict(t="rrect", x=-19, y=60, w=38, h=74, r=4, c="pad", l="back"),       # backrest
        dict(t="rrect", x=-22, y=50, w=44, h=8, r=3, c="pad", l="back"),        # seat
        dict(t="rect", x=-3, y=5, w=6, h=45, c="frame", l="back"),
        dict(t="circle", x=0, y=194, r=4, c="pulley", l="back"),
        dict(t="rrect", x=-22, y=52, w=44, h=9, r=4, c="roller", l="front"),     # upper rollers (both)
    ],
)
