package org.example;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Main {
    private static final int MESSAGE_SIZE_BITS = 255;
    private static final String POLYNOMIAL_HEX = "5D6DCB";

    public static void main(String[] args) {
        try {
            // Чтение стеганоконтейнера
            BufferedImage image = ImageIO.read(new File("image.bmp"));

            // Пример входного сообщения (255 бит)
            String message = "0987654321";
            if (message.length() > MESSAGE_SIZE_BITS) {
                System.err.println("Сообщение слишком длинное.");
                return;
            }

            // Вставка сообщения
            embedMessage(image, message);

            // Извлечение сообщения
            String extractedMessage = extractMessage(image);

            // Проверка CRC 24
            if (checkCRC(extractedMessage, POLYNOMIAL_HEX)) {
                System.out.println("Сообщение успешно извлечено и прошло проверку CRC 24.");
                System.out.println("Извлеченное сообщение: " + extractedMessage);
            } else {
                System.err.println("Ошибка: сообщение не прошло проверку CRC 24.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Вставка сообщения в изображение
    private static void embedMessage(BufferedImage image, String message) {
        int width = image.getWidth();
        int height = image.getHeight();
        int messageLength = message.length();
        int pixelIndex = 0;

        // Внедряем биты сообщения в изображение
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (pixelIndex < messageLength) {
                    int pixel = image.getRGB(x, y);
                    int alpha = (pixel >> 24) & 0xFF;
                    int red = (pixel >> 16) & 0xFF;
                    int green = (pixel >> 8) & 0xFF;
                    int blue = pixel & 0xFF;

                    int bitToEmbed = Integer.parseInt(message.charAt(pixelIndex) + "");
                    red = (red & 0xFE) | bitToEmbed;
                    image.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
                    pixelIndex++;
                }
            }
        }

        // Добавляем CRC 24 контрольную сумму к сообщению и внедряем ее
        String messageWithCRC = message + calculateCRC(message, POLYNOMIAL_HEX);
        for (int i = 0; i < messageWithCRC.length(); i++) {
            if (pixelIndex < messageWithCRC.length()) {
                int pixel = image.getRGB(pixelIndex % width, pixelIndex / width);
                int alpha = (pixel >> 24) & 0xFF;
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                int bitToEmbed = Integer.parseInt(messageWithCRC.charAt(i) + "");
                red = (red & 0xFE) | bitToEmbed;
                image.setRGB(pixelIndex % width, pixelIndex / width, (alpha << 24) | (red << 16) | (green << 8) | blue);
                pixelIndex++;
            }
        }
    }


    // Извлечение сообщения из изображения
    private static String extractMessage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        StringBuilder extractedMessage = new StringBuilder();
        int pixelIndex = 0;

        while (pixelIndex < width * height) {
            int pixel = image.getRGB(pixelIndex % width, pixelIndex / width);
            int red = (pixel >> 16) & 0xFF;
            int lsb = red & 0x01;
            extractedMessage.append(lsb);

            // Проверяем, если извлеченное сообщение завершилось сигнатурой CRC 24
            if (extractedMessage.length() >= 24 && extractedMessage.substring(extractedMessage.length() - 24).equals(POLYNOMIAL_HEX)) {
                break;
            }

            pixelIndex++;
        }

        if (extractedMessage.length() < 24) {
            System.err.println("Ошибка: CRC 24 сигнатура не найдена.");
            return null;
        }

        String extractedCRC = extractedMessage.substring(extractedMessage.length() - 24);
        extractedMessage.delete(extractedMessage.length() - 24, extractedMessage.length());

        // Проверяем CRC 24 контрольную сумму
        if (checkCRC(extractedMessage.toString(), extractedCRC)) {
            return extractedMessage.toString();
        } else {
            System.err.println("Ошибка: сообщение не прошло проверку CRC 24.");
            return null;
        }
    }

    // Вычисление CRC 24 контрольной суммы
    private static String calculateCRC(String message, String polynomialHex) {
        int crc = 0x000000;

        for (int i = 0; i < message.length(); i++) {
            int data = (int) message.charAt(i) & 0xFF;
            crc ^= data << 16;
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x800000) != 0) {
                    crc = (crc << 1) ^ Integer.parseInt(polynomialHex, 16);
                } else {
                    crc = (crc << 1);
                }
            }
        }

        String crcHexString = Integer.toHexString(crc);
        // Дополнить нулями до 6 символов
        while (crcHexString.length() < 6) {
            crcHexString = "0" + crcHexString;
        }
        return crcHexString;
    }


    // Проверка CRC 24
    private static boolean checkCRC(String message, String receivedCRC) {
        String calculatedCRC = calculateCRC(message, POLYNOMIAL_HEX);
        return calculatedCRC.equals(receivedCRC);
    }
}
