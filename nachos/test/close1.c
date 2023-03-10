/* close1.c
** open a file and then close it
*/
#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int main() {
    char *str = "output.txt";
    int fd = open(str);
    int res1 = close(fd);
    assert(res1 == 0);
    if (res1 == 0) { printf("Test Passed: Close Correctly\n"); }
    else { printf("Test Failed: Error in Close\n"); }
    int res2 = close(fd);
    assert(res2 == -1);
    if (res2 == -1) { printf("Test Passed: Close failed when closing a closed fd\n"); }
    else { printf("Test Failed: when closing a closed fd\n"); }
    return 0;
}
