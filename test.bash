echo $SEMTK
SEMTK="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd -W 2>/dev/null)"
if [ -z $SEMTK ]
then
        SEMTK="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
fi
echo $SEMTK

echo fine.


