mvn install:install-file -Dfile=libs/commons-codec.jar -DgroupId=org.apache.commons.codec -DartifactId=CommonsCodec -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile=libs/guava-r09.jar -DgroupId=com.google.common -DartifactId=Guava -Dversion=0.90 -Dpackaging=jar
mvn install:install-file -Dfile=libs/sqlcipher.jar -DgroupId=info.guardianproject.database -DartifactId=SqlCipher -Dversion=1.1.0 -Dpackaging=jar

