FROM openjdk:8-alpine

COPY target/uberjar/xyloobservations.jar /xyloobservations/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/xyloobservations/app.jar"]
