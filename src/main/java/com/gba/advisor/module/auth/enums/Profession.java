package com.gba.advisor.module.auth.enums;

import lombok.Getter;

@Getter
public enum Profession {
    SHEN_XIANG("神相", "avatars/shen_xiang.png"),
    SU_WEN    ("素问", "avatars/su_wen.png"),
    CHAO_GUANG("潮光", "avatars/chao_guang.png"),
    HONG_YIN  ("鸿音", "avatars/hong_yin.png"),
    LONG_YIN  ("龙吟", "avatars/long_yin.png"),
    CANG_LAN  ("沧澜", "avatars/cang_lan.png"),
    XUAN_JI   ("玄机", "avatars/xuan_ji.png"),
    TIE_YI    ("铁衣", "avatars/tie_yi.png"),
    SUI_MENG  ("碎梦", "avatars/sui_meng.png"),
    JIU_LING  ("九灵", "avatars/jiu_ling.png"),
    XUE_HE    ("血河", "avatars/xue_he.png");

    private final String label;
    private final String avatarOssKey;

    Profession(String label, String avatarOssKey) {
        this.label = label;
        this.avatarOssKey = avatarOssKey;
    }

    public static Profession fromLabel(String label) {
        for (Profession p : values()) {
            if (p.label.equals(label)) return p;
        }
        throw new IllegalArgumentException("未知职业：" + label);
    }
}