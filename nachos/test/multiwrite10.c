#include "syscall.h"
#define N 8
int main() {
    char *progrm = "write10.coff";
    int retureValues[N];
    int pids[N];
    for (int i = 0; i < N; i += 1) {
        pids[i] = exec(progrm, 0, 0);
    }
    for (int i = 0; i < N; i += 1) {
        join(pids[i], retureValues + i);
    }
    for (int i = 0; i < N; i += 1) {
        if (retureValues[i] != 0) {
            exit(i + 1000);
        }
    }
    return 0;
}