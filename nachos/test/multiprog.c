#include "syscall.h"
#include "stdio.h"
#define N 8
int main(int argc, char** argv) {
    char *progrm = argv[0];
    printf("Running %d %s.coff processes.", N, progrm);
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
            exit(retureValues[i]);
        }
    }
    return 0;
}