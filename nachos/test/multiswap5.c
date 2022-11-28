#include "syscall.h"
#include "stdio.h"
#define N 16
int main() {
    printf("Running %d swap5.coff processes.");
    int retureValues[N];
    int pids[N];
    for (int i = 0; i < N; i += 1) {
        pids[i] = exec("swap5.coff", 0, 0);
    }
    for (int i = 0; i < N; i += 1) {
        join(pids[i], retureValues + i);
    }
    for (int i = 0; i < N; i += 1) {
        if (retureValues[i] != -1000) {
            exit(retureValues[i]);
        }
    }
    return 0;
}