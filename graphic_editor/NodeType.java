package org.example.graphic_editor;

/**
 * Типи вузлів: Генератор, Черга, Пристрій, Розгалуження.
 */
public enum NodeType {
    GENERATOR,      // Генератор запитів
    QUEUE,          // Черга
    SERVICE_DEVICE, // Пристрій (один канал)
    ROUTE_SPLIT     // Розгалуження маршруту
}