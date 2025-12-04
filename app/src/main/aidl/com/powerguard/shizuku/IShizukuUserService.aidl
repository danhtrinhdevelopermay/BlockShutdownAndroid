package com.powerguard.shizuku;

interface IShizukuUserService {
    void destroy() = 16777114;
    void exit() = 1;
    boolean blockPowerAction() = 2;
    boolean allowPowerAction() = 3;
}
