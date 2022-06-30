FROM amazoncorretto:11
EXPOSE 80
ADD /target/jiren-1.0.0.jar jiren.jar
ENTRYPOINT ["java","-jar","jiren.jar"]