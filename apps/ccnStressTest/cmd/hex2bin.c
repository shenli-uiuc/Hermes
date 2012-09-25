#include<stdio.h>
#include<stdlib.h>
#include<string.h>

int main(int argc, char *argv[]){
    int i = 0, len = 0;
    unsigned char * strHex = argv[1];
    unsigned char * strPath = argv[2];
    unsigned char tmpChar;
    len = strlen(strHex);
    printf("string length is %d\n", len);
    freopen(strPath, "w", stdout);
    for(i = 0 ;i < len; ++i){
        if(strHex[i] <= '9')
            tmpChar |= strHex[i] - '0';
        else
            tmpChar |= 10 + strHex[i] - 'a';
        if(i%2){
            printf("%c", tmpChar);
            tmpChar = 0;
        }
        tmpChar = tmpChar << 4;
    }

    return 0;
}
