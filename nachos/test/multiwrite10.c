#include "syscall.h"

int main() {
    char *progrm = "write10.coff";
    for (int i = 0; i < 16; i += 1) {
        exec(progrm, 0, 0);
    }
    return 0;
}