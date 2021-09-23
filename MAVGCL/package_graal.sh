#! /bin/bash

export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-ce-java16-21.2.0/Contents/Home
export PATH=/Library/Java/JavaVirtualMachines/graalvm-ce-java16-21.2.0/Contents/Home/bin:$PATH

echo $JAVA_HOME

jlink --no-header-files --no-man-pages --compress=2 --strip-debug --add-modules java.se,jdk.httpserver,javafx.controls,javafx.fxml,org.graalvm.sdk,org.graalvm.locator,jdk.internal.le,jdk.internal.ed,jdk.hotspot.agent,jdk.internal.vm.ci,jdk.internal.vm.compiler,jdk.internal.vm.compiler.management,jdk.internal.jvmstat,jdk.internal.ed,jdk.internal.opt --output ./target/custom_jre --module-path ./jmods


jpackage --input target/deploy/ --name MAVGAnalysis --main-jar MAVGCL-0.8.0.jar --main-class com.comino.flight.MainApp --type dmg --icon target/deploy/MAVGAnalysis.icns --runtime-image ./target/custom_jre --java-options '-Djava.library.path=./native -XX:+UnlockExperimentalVMOptions -XX:+UseJVMCICompiler -XX:+EnableJVMCI -Xmx8G '