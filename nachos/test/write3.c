/* write2.c
 ** write str into an exist file with invalid arguments -fd.
 */
 #include "syscall.h"
 #include "stdio.h"
 #include "stdlib.h"
 int main() {
     char *inputStr = "write something here";
     int fd = 10;
     int written = write(fd, inputStr, strlen(inputStr));
     if (written == -1) {
         printf("Should be an error, pass.");
         exit(-1);
     } else {
         printf("send %d", written);
     }
     return 0;
 }