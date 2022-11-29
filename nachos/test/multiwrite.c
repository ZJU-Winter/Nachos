#include "syscall.h"
#include "stdio.h"
#define N 3
int main() {
    printf("Running %d write10.coff processes.\n", N);
    exec("write10.coff", 0, 0);
    exec("write10_1.coff", 0, 0);
    exec("write10_2.coff", 0, 0);
}