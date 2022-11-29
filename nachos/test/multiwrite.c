#include "syscall.h"
#include "stdio.h"
#define N 3
int main() {
    printf("Running %d write10.coff processes.\n", N);
    exec("mywrite10.coff", 0, 0);
    exec("mywrite10_1.coff", 0, 0);
    exec("mywrite10_2.coff", 0, 0);
}