// тест для лексического анализатора с ошибками

/* типы и константы */
short int gNum8=001234567;
long gNum10 = 89, gArray[0XabcDEF];
int gNum16 = 0x123456789ABCdef;
char while77while;

/*/
  * класс
  //*/
class _1a55 { int x; } _1/*abc//*/, _2 = ((_1)); // /*/ /****/ */

int main() { // main
    00099; // неверная восьмиричная константа
    0XXf; // неверная шестнадцатиричная константа
    ! 4 !/ 5; 4 != 5; // после ! может быть только =
    while (while77while = -1) {
        char f;
        gArray[
            //f == f <= f >= f > f < f << f >> f + f - f * f / f % --(++f)
            <=== >=== <<< >>> +++ -- - * / %
        ] = f;
    }
    return _2.x * (-1 / 2);

    #$@~^&`'\"?| // прочие неподдерживаемые лексемы
}	/****** незакрытый многострочный комментарий