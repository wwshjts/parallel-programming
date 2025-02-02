
set -euo pipefail

echo "Sample"
javac Sample.java

java Sample & 

PID="$!"

echo "PID=$PID"

sleep 1
jstack -l $PID


