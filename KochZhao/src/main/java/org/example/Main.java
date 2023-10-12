package org.example;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import javax.imageio.ImageIO;

public class Main {

    // Внедрение сообщения в изображение
    public static void embedMessage(String inputImageFile, String outputImageFile, byte[] message) {
        try {
            // Загрузка изображения
            BufferedImage image = ImageIO.read(new File(inputImageFile));

            int width = image.getWidth();
            int height = image.getHeight();

            int messageLength = message.length;

            // Проверка, что сообщение может быть встроено в изображение
            if (messageLength * 8 > width * height) {
                System.err.println("Сообщение слишком большое для данного изображения.");
                return;
            }

            int messageIndex = 0;

            // Перебор пикселей изображения и встраивание битов сообщения
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = image.getRGB(x, y);
                    int alpha = (pixel >> 24) & 0xFF;
                    int red = (pixel >> 16) & 0xFF;
                    int green = (pixel >> 8) & 0xFF;
                    int blue = pixel & 0xFF;

                    if (messageIndex < messageLength) {
                        // Получение следующего байта сообщения
                        byte nextByte = message[messageIndex];
                        for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
                            // Встраивание битов сообщения в младшие биты компонентов RGB
                            int bit = (nextByte >> bitIndex) & 1;
                            red = (red & 0xFE) | bit;
                            messageIndex++;
                            if (messageIndex >= messageLength) {
                                break;
                            }
                        }
                    }

                    // Обновление пикселя с встроенным битом
                    int newPixel = (alpha << 24) | (red << 16) | (green << 8) | blue;
                    image.setRGB(x, y, newPixel);
                }
            }

            // Сохранение изображения с встроенным сообщением
            ImageIO.write(image, "BMP", new File(outputImageFile));
            System.out.println("Сообщение успешно встроено в изображение.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Извлечение сообщения из изображения
    public static byte[] extractMessage(String inputImageFile, int messageLength) {
        try {
            // Загрузка изображения
            BufferedImage image = ImageIO.read(new File(inputImageFile));

            int width = image.getWidth();
            int height = image.getHeight();

            byte[] extractedMessage = new byte[messageLength];
            int messageIndex = 0;

            // Перебор пикселей и извлечение младших битов
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = image.getRGB(x, y);
                    int red = (pixel >> 16) & 0xFF;

                    for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
                        if (messageIndex >= messageLength) {
                            return extractedMessage;
                        }

                        int bit = (red >> bitIndex) & 1;
                        extractedMessage[messageIndex] = (byte) ((extractedMessage[messageIndex] << 1) | bit);
                        messageIndex++;
                    }
                }
            }

            return extractedMessage;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    // Вычисление CRC24 контрольной суммы для сообщения
    public static long calculateCRC24(byte[] message) {
        Checksum checksum = new CRC32();
        checksum.update(message, 0, message.length);
        long crcValue = checksum.getValue();
        // Применение ограничения на 24 бита
        return crcValue & 0xFFFFFF;
    }

    public static void main(String[] args) {
        String inputImageFile = "image.bmp";
        String outputImageFile = "output_image.bmp";

        // Входное сообщение
// Входное сообщение
        byte[] message = "1234567890".getBytes();

// Встраивание контрольной суммы CRC 24 в сообщение
        long crcValue = calculateCRC24(message);
        byte[] crcBytes = new byte[3];
        crcBytes[0] = (byte) ((crcValue >> 16) & 0xFF);
        crcBytes[1] = (byte) ((crcValue >> 8) & 0xFF);
        crcBytes[2] = (byte) (crcValue & 0xFF);

// Добавление CRC к сообщению
        byte[] messageWithCRC = new byte[message.length + 3];
        System.arraycopy(message, 0, messageWithCRC, 0, message.length);
        System.arraycopy(crcBytes, 0, messageWithCRC, message.length, 3);

// Встраивание сообщения в изображение
        embedMessage(inputImageFile, outputImageFile, messageWithCRC);


        // Извлечение сообщения из изображения
        byte[] extractedMessage = extractMessage(outputImageFile, messageWithCRC.length);

        // Проверка CRC
        long extractedCRC = calculateCRC24(extractedMessage);
        if (extractedCRC == crcValue) {
            System.out.println("CRC проверка прошла успешно.");
        } else {
            System.out.println("Ошибка CRC проверки.");
        }
    }
}
