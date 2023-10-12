package org.example;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Main {
    private static final int MESSAGE_SIZE_BITS = 255;
    private static final String POLYNOMIAL_HEX = "5D6DCB";

    // Метод для конвертации строки в бинарное представление (ASCII-коды)
    private static String convertToBinary(String input) {
        StringBuilder binaryMessage = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int asciiValue = (int) c;
            String binaryValue = Integer.toBinaryString(asciiValue);
            // Дополнить нулями слева, чтобы получить 8-битное представление
            while (binaryValue.length() < 8) {
                binaryValue = "0" + binaryValue;
            }
            binaryMessage.append(binaryValue);
        }
        return binaryMessage.toString();
    }

    public static void main(String[] args) {
        try {
            // Чтение стеганоконтейнера
            BufferedImage image = ImageIO.read(new File("image.bmp"));

            // Пример входного сообщения (строка с цифрами и буквами)
            String message = "Hello123";
            String binaryMessage = convertToBinary(message);

            // Вставка сообщения
            embedMessage(image, binaryMessage);

            // Извлечение сообщения
            String extractedBinaryMessage = extractMessage(image);

            // Проверка CRC 24
            if (extractedBinaryMessage != null) {
                System.out.println("Сообщение успешно извлечено и прошло проверку CRC 24.");
                String extractedMessage = convertBinaryToString(extractedBinaryMessage);
                System.out.println("Извлеченное сообщение: " + extractedMessage);
            } else {
                System.err.println("Ошибка: сообщение не прошло проверку CRC 24 или не найдено.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод для конвертации бинарного представления в строку
    private static String convertBinaryToString(String binary) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < binary.length(); i += 8) {
            String byteStr = binary.substring(i, i + 8);
            int byteValue = Integer.parseInt(byteStr, 2);
            char c = (char) byteValue;
            text.append(c);
        }
        return text.toString();
    }


    // Вставка сообщения в изображение
    private static void embedMessage(BufferedImage image, String message) {
        int width = image.getWidth();
        int height = image.getHeight();
        int pixelIndex = 0;

        for (int i = 0; i < message.length(); i++) {
            char bitToEmbed = message.charAt(i);
            int pixel = image.getRGB(pixelIndex % width, pixelIndex / width);
            int alpha = (pixel >> 24) & 0xFF;
            int red = (pixel >> 16) & 0xFF;
            int newRed = (red & 0xFE) | Character.getNumericValue(bitToEmbed);
            int newPixel = (alpha << 24) | (newRed << 16) | (red << 8) | (pixel & 0xFF);
            image.setRGB(pixelIndex % width, pixelIndex / width, newPixel);
            pixelIndex++;
        }

        // Добавляем CRC 24 контрольную сумму к сообщению и внедряем ее
        String messageWithCRC = message + calculateCRC(message, POLYNOMIAL_HEX);
        for (int i = 0; i < messageWithCRC.length(); i++) {
            char bitToEmbed = messageWithCRC.charAt(i);
            int pixel = image.getRGB(pixelIndex % width, pixelIndex / width);
            int alpha = (pixel >> 24) & 0xFF;
            int red = (pixel >> 16) & 0xFF;
            int newRed = (red & 0xFE) | Character.getNumericValue(bitToEmbed);
            int newPixel = (alpha << 24) | (newRed << 16) | (red << 8) | (pixel & 0xFF);
            image.setRGB(pixelIndex % width, pixelIndex / width, newPixel);
            pixelIndex++;
        }
    }


    // Извлечение сообщения из изображения
    private static String extractMessage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int pixelIndex = 0;
        StringBuilder extractedMessage = new StringBuilder();

        while (extractedMessage.length() < MESSAGE_SIZE_BITS) {
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

        if (extractedMessage.length() >= 24) {
            String extractedCRC = extractedMessage.substring(extractedMessage.length() - 24);
            extractedMessage.delete(extractedMessage.length() - 24, extractedMessage.length());

            // Проверяем CRC 24 контрольную сумму
            if (checkCRC(extractedMessage.toString(), extractedCRC)) {
                return extractedMessage.toString();
            } else {
                System.err.println("Ошибка: сообщение не прошло проверку CRC 24.");
            }
        } else {
            System.err.println("Ошибка: CRC 24 сигнатура не найдена.");
        }

        return null;
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
