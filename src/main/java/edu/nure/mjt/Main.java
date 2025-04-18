package edu.nure.mjt;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Phaser;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    static class MatrixTask implements Runnable {

        //Поля класу
        private final int startRow;
        private final int endRow;
        private final double[][] MatrixA, MatrixB, MatrixC;
        private final int n, p;

        //Конструктор об'єкту класу
        public MatrixTask(int startRow, int endRow, double[][] MatrixA, double[][] MatrixB,
                          double[][] MatrixC, int n, int p) {
            //Рядок матриці C, з якого починаються обчислення одним потоком з пулу
            this.startRow = startRow;
            //Рядок матриці-результату C, на якому завершуються обчислення одним потоком з пулу
            this.endRow = endRow;
            //Матриці-множники + матриця-результат
            this.MatrixA = MatrixA;
            this.MatrixB = MatrixB;
            this.MatrixC = MatrixC;
            //Кількість стовпців матриці A та рядків матриці B
            this.n = n;
            //Кількість стовпців в матрицях B, C
            this.p = p;
        }
        //Операція множення матриць різними потоками пулу
        @Override
        public void run() {
            for(int i = startRow; i < endRow; i++) {
                for(int j = 0; j < p; j++){
                    MatrixC[i][j] = 0;
                    for(int k = 0; k < n; k++){
                        MatrixC[i][j] += MatrixA[i][k] * MatrixB[k][j];
                    }
                }
            }
        }
    }

    //Клас задачі з пріоритетом
    //static class PriorityTask implements Runnable, Comparable<PriorityTask> {
        //Змінна пріоритету виконуваної задачі потоком (наприклад, threadId)
        //private final int priority;
        //Змінна задачі у потоці
        //private final Runnable task;

        //Конструктор об'єкта виконуваної задачі з пріоритетом
        //public PriorityTask(int priority, Runnable task) {
            //this.priority = priority;
            //this.task = task;
        //}

        //@Override
        //public void run() {
            //task.run();
        //}

        //@Override
        //public int compareTo(PriorityTask other) {
            //Виконання задачі з меншим priority (номером потоку) раніше
            //return Integer.compare(this.priority, other.priority);
        //}
    //}

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("Оберіть дію:");
            System.out.println("1. Mutex");
            System.out.println("2. Semaphore");
            System.out.println("3. Atomic: not synchronized");
            System.out.println("4. Atomic: synchronized with Phaser");
            //System.out.println("5. Atomic: synchronized with Queue");
            System.out.println("5. Pull-Thread ExecuteService");
            System.out.println("6. Matrix Multiplication");
            System.out.print("Введіть номер дії (1-6): ");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    System.out.print("Введіть кількість потоків (n): ");
                    int n = scanner.nextInt();
                    System.out.print("Введіть нижню межу часу t1 (мс): ");
                    int t1 = scanner.nextInt();
                    System.out.print("Введіть верхню межу часу t2 (мс): ");
                    int t2 = scanner.nextInt();

                    if (n <= 0 || t1 < 0 || t2 < t1) {
                        System.out.println("Некоректні дані. Спробуйте ще раз.");
                        break;
                    }
                    ReentrantLock mutex = new ReentrantLock();
                    Thread[] threads = new Thread[n];
                    Random random = new Random();
                    for (int i = 0; i < n; i++) {
                        final int threadId = i + 1;
                        threads[i] = new Thread(() -> {
                            int sleepTime = t1 + random.nextInt(t2 - t1 + 1);
                            mutex.lock();
                            try {
                                System.out.println("Потік " + Thread.currentThread().getName() + " захопив м'ютекс.");
                                System.out.println("Потік " + Thread.currentThread().getName() + " переходить у неактивний стан на " + sleepTime + " мс.");
                                try {
                                    Thread.sleep(sleepTime);
                                } catch (InterruptedException e) {
                                    System.err.println("Потік " + Thread.currentThread().getName() + " перервано під час сну.");
                                }
                                System.out.println("Потік " + Thread.currentThread().getName() + " прокинувся.");
                            } finally {
                                System.out.println("Потік " + Thread.currentThread().getName() + " звільняє м'ютекс.");
                                mutex.unlock();
                            }
                        }, "Thread-" + threadId);
                        threads[i].start();
                    }

                    for (Thread thread : threads) {
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            System.err.println("Головний потік перервано.");
                        }
                    }
                    System.out.println("Усі потоки завершили роботу.");
                    break;

                case 2:
                    // Семафор із 4 дозволами
                    Semaphore semaphore = new Semaphore(4);
                    // Лічильник для відстеження залишкових місць
                    AtomicInteger remainingPlaces = new AtomicInteger(4);
                    // Лок для синхронізації виводу
                    ReentrantLock outputLock = new ReentrantLock();

                    System.out.print("Введіть кількість потоків (n): ");
                    int n2 = scanner.nextInt();
                    System.out.print("Введіть нижню межу часу t1 (мс): ");
                    int t12 = scanner.nextInt();
                    System.out.print("Введіть верхню межу часу t2 (мс): ");
                    int t22 = scanner.nextInt();

                    if (n2 <= 0 || t12 < 0 || t22 <= t12) {
                        System.out.println("Некоректні дані. Спробуйте ще раз.");
                        continue;
                    }

                    // Створення n2 потоків
                    Thread[] threads2 = new Thread[n2];
                    Random random2 = new Random();

                    for (int i = 0; i < n2; i++) {
                        final int threadId = i + 1;
                        threads2[i] = new Thread(() -> {
                            int sleepTime = t12 + random2.nextInt(t22 - t12 + 1);
                            //Ім'я активного потоку
                            String threadName = Thread.currentThread().getName();
                            try {
                                //Вивід повідомлення про спробу захоплення семафора активним потоком
                                System.out.println("Потік " + threadName + " намагається отримати семафор");
                                //Захоплення семафора
                                semaphore.acquire();
                                //Зменшуємо лічильник вільних у семафорі місць на 1
                                int placesLeft = remainingPlaces.decrementAndGet();
                                //Лише один потік має доступ до консолі для виводу повідомлення
                                outputLock.lock();
                                System.out.println("Потік " + threadName + " отримав семафор. Лишилося " + placesLeft + " місце");
                                outputLock.unlock();
                                //Тут вже нема синхронізації, тому рядки виводяться в довільному порядку
                                System.out.println("Потік " + threadName + " переходить у неактивний стан на " + sleepTime + " мс.");

                                try {
                                    //Активний потік засинає
                                    Thread.sleep(sleepTime);
                                } catch (InterruptedException e) {
                                    System.err.println("Потік " + threadName + " перервано під час сну");
                                }
                                //Додаємо м'ютекс для того, щоб вивід був таким: спочатку потік прокинувся, потім він звільнив семафор,
                                //а лише потім новий потік зайняв місце вільне у семафорі
                                outputLock.lock();
                                System.out.println("Потік " + threadName + " прокинувся");
                                // Звільнення семафора потоком, що прокинувся
                                semaphore.release();
                                //Збільшуємо лічильник після звільнення місця в семафорі
                                int newPlaces = remainingPlaces.incrementAndGet();
                                System.out.println("Потік " + threadName + " звільняє семафор. Кількість дозволів стає: " + newPlaces);
                                outputLock.unlock();
                            } catch (Exception e) {
                                System.err.println("Потік " + threadName + " перервано: " + e.getMessage());
                            }
                        }, "Thread-" + threadId);
                        threads2[i].start();
                    }

                    //Очікування завершення всіх потоків
                    for (Thread thread : threads2) {
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            System.err.println("Головний потік перервано.");
                        }
                    }
                    System.out.println("Усі потоки завершили роботу.");
                    break;

                case 3:
                    System.out.print("Введіть ціле число, з яким будуть працювати потоки: ");
                    int IntNum = scanner.nextInt();
                    //Створення глобальної змінної для синхронізації через AtomicInteger
                    AtomicInteger Counter = new AtomicInteger(IntNum);
                    System.out.print("Введіть кількість потоків (n): ");
                    int n3 = scanner.nextInt();
                    if (n3 <= 0) {
                        System.out.println("Некоректні дані. Спробуйте ще раз.");
                        continue;
                    }
                    //Створення n3 потоків
                    Thread[] threads3 = new Thread[n3];
                    for (int i = 0; i < n3; i++) {
                        final int threadId = i + 1;
                        threads3[i] = new Thread(() -> {
                            String threadName = Thread.currentThread().getName();
                            try {
                                //Збільшення значення числа створеними потоками
                                int newValueIncrement = Counter.incrementAndGet();
                                System.out.println("Потік " + threadName + " збільшив значення до " + newValueIncrement);

                                //Зменшення значення числа
                                int newValueDecrement = Counter.decrementAndGet();
                                System.out.println("Потік " + threadName + " зменшив значення до " + newValueDecrement);

                                //Зміна (встановлення нового значення)
                                int newValueSet = Counter.get() + threadId * 10;
                                int oldValue = Counter.getAndSet(newValueSet);
                                System.out.println("Потік " + threadName + " змінив значення з " + oldValue + " на " + newValueSet);
                            } catch (Exception e) {
                                System.err.println("Потік " + threadName + " перервано: " + e.getMessage());
                            }
                        }, "Thread-" + threadId);

                        threads3[i].start();
                    }
                    //Очікування завершення всіх потоків
                    for (Thread thread : threads3) {
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            System.err.println("Головний потік перервано.");
                        }
                    }
                    System.out.println("Усі потоки завершили роботу. Остаточне значення: " + Counter.get());
                    break;

                case 4:

                    System.out.print("Введіть ціле число, з яким будуть працювати потоки: ");
                    int IntNum2 = scanner.nextInt();
                    // Створення глобальної змінної для синхронізації через AtomicInteger
                    AtomicInteger Counter2 = new AtomicInteger(IntNum2);
                    System.out.print("Введіть кількість потоків (n): ");
                    int n32 = scanner.nextInt();
                    if (n32 <= 0) {
                        System.out.println("Некоректні дані. Спробуйте ще раз.");
                        continue;
                    }
                    // Створення Phaser для синхронізації потоків
                    Phaser phaser = new Phaser(n32);
                    // Створення n3 потоків
                    Thread[] threads32 = new Thread[n32];
                    for (int i = 0; i < n32; i++) {
                        final int threadId = i + 1;
                        threads32[i] = new Thread(() -> {
                            String threadName = Thread.currentThread().getName();
                            try {
                                //Збільшення значення числа
                                int newValueIncrement = Counter2.incrementAndGet();
                                System.out.println("Потік " + threadName + " збільшив значення до " + newValueIncrement);
                                //Чекаємо, поки всі потоки збільшать число та про це
                                //буде виведеним повідомлення на консоль
                                phaser.arriveAndAwaitAdvance();

                                //Зменшення значення числа
                                int newValueDecrement = Counter2.decrementAndGet();
                                System.out.println("Потік " + threadName + " зменшив значення до " + newValueDecrement);
                                //Чекаємо, поки всі потоки зменшать число та про це
                                //буде виведеним повідомлення на консоль
                                phaser.arriveAndAwaitAdvance();

                                //Зміна (встановлення нового значення)
                                int newValueSet = Counter2.get() + threadId * 10;
                                int oldValue = Counter2.getAndSet(newValueSet);
                                System.out.println("Потік " + threadName + " змінив значення з " + oldValue + " на " + newValueSet);
                                //Чекаємо, поки всі потоки змінять зміншене вище число та про це
                                //будуть виведеними повідомлення на консоль
                                phaser.arriveAndAwaitAdvance();
                            } catch (Exception e) {
                                System.err.println("Потік " + threadName + " перервано: " + e.getMessage());
                            }
                        }, "Thread-" + threadId);
                        threads32[i].start();
                    }
                    // Очікування завершення всіх потоків
                    for (Thread thread : threads32) {
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            System.err.println("Головний потік перервано.");
                        }
                    }
                    System.out.println("Усі потоки завершили роботу. Остаточне значення: " + Counter2.get());
                    break;

                case 5:
                    //Вхідні дані
                    System.out.print("Введіть розмір пулу потоків k: ");
                    int k = scanner.nextInt();
                    System.out.print("Введіть загальну кількість потоків n: ");
                    int n4 = scanner.nextInt();
                    System.out.print("Введіть нижню межу часу t1 (мс): ");
                    int t14 = scanner.nextInt();
                    System.out.print("Введіть верхню межу часу t2 (мс): ");
                    int t24 = scanner.nextInt();

                    if (k <= 0 || n4 < k || t14 < 0 || t24 <= t14) {
                        System.out.println("Некоректні дані. Спробуйте ще раз.");
                        continue;
                    }

                    //ExecutorService - інтерфейіс
                    //Створення пулу із k потоків - "робітників"
                    ExecutorService threadPool2 = Executors.newFixedThreadPool(k);
                    Random random4 = new Random();

                    // Запускаємо n4 потоків
                    for (int i = 0; i < n4; i++) {
                        final int taskId = i + 1;
                        //Додаємо завдання в пул потоків
                        threadPool2.submit(() -> {
                            int sleepTime = t14 + random4.nextInt(t24 - t14 + 1);
                            String taskName = "Task-" + taskId;
                            //Вивід повідомлення про початок виконання завдання
                            synchronized (System.out) {
                                System.out.println("Завдання " + taskName + " почало виконання");
                            }
                            //Потік засинає
                            try {
                                Thread.sleep(sleepTime);
                            } catch (InterruptedException e) {
                                synchronized (System.out) {
                                    System.err.println("Завдання " + taskName + " перервано під час виконання");
                                }
                            }

                            //Повідомлення про завершення виконання завдання
                            synchronized (System.out) {
                                System.out.println("Завдання " + taskName + " завершило виконання після " + sleepTime + " мс");
                            }
                        });
                    }
                    //Завершення пулу і очікування завершення всіх завдань
                    threadPool2.shutdown();
                    try {
                        if (!threadPool2.awaitTermination(60, TimeUnit.SECONDS)) {
                            //Якщо завдання з пулу не завершилися за 60 секунд, то вони зупиняються примусово
                            threadPool2.shutdownNow();
                            synchronized (System.out) {
                                System.out.println("Пул потоків примусово зупинений через тайм-аут");
                            }
                        }
                    } catch (InterruptedException e) {
                        threadPool2.shutdownNow();
                        synchronized (System.out) {
                            System.err.println("Головний потік перервано під час очікування завершення пулу");
                        }
                    }
                    synchronized (System.out) {
                        System.out.println("Усі завдання завершили роботу.");
                    }
                    break;

                case 6:
                    //Вхідні дані
                    System.out.print("Введіть к-сть рядків m матриці A: ");
                    int m = scanner.nextInt();
                    System.out.print("Введіть к-сть стовпців n матриці A/к-сть рядків n матриці B: ");
                    int n6 = scanner.nextInt();
                    System.out.print("Введіть к-сть стовпців p матриці C: ");
                    int p = scanner.nextInt();
                    System.out.print("Введіть к-сть рядків для обчислення 1 потоком N матриці C: ");
                    int N = scanner.nextInt();
                    //Очищення буферу перед введенням елементів матриць
                    scanner.nextLine();

                    //Перевірка коректності вхідних даних
                    if (m <= 0 || n6 <= 0 || p <= 0 || N <= 0) {
                        System.out.println("Некоректні дані. Спробуйте ще раз.");
                        break;
                    }

                    //Матриці для задачі
                    double[][] MatrixA = new double[m][n6];
                    double[][] MatrixB = new double[n6][p];
                    double[][] MatrixC = new double[m][p];
                    //scanner.nextLine();

                    //Введення елементів матриць-множників
                    System.out.println("Введіть елементи матриці A");
                    for (int i = 0; i < m; i++) {
                        for (int j = 0; j < n6; j++) {
                            MatrixA[i][j] = scanner.nextDouble();
                        }
                    }
                    System.out.println("Введіть елементи матриці B");
                    for (int i = 0; i < n6; i++) {
                        for (int j = 0; j < p; j++) {
                            MatrixB[i][j] = scanner.nextDouble();
                        }
                    }
                    //Момент часу початку обчислень
                    //long startTime = System.nanoTime();

                    //Pозмір пулу потоків
                    int NumThreads = (int) Math.ceil((double) m / N);
                    ExecutorService threadPoolMulti = Executors.newFixedThreadPool(NumThreads);

                    //Виконання завдання
                    for (int i = 0; i <= m; i += N) {
                        int startRow = i;
                        int endRow = Math.min(m, i + N);
                        //Додаємо задачу в пул
                        threadPoolMulti.submit(new MatrixTask(startRow, endRow, MatrixA, MatrixB, MatrixC, n6, p));
                    }
                    //Припинення роботи пулу потоків
                    threadPoolMulti.shutdown();
                    try {
                        //Примусове припинення роботи пулу потоків, якщо завдання
                        // після завершення роботи виконують довше хвилини
                        if (!threadPoolMulti.awaitTermination(60, TimeUnit.SECONDS)) {
                            threadPoolMulti.shutdownNow();
                            System.out.println("Роботу пулу примусово зупинено через тайм-аут");
                        }
                    } catch (InterruptedException e) {
                        System.out.println("Роботу пулу перервано через помилку");
                    }
                    //Виведення результуючої матриці
                    System.out.println("Результуюча матриця C");
                    for (int i = 0; i < m; i++) {
                        for (int j = 0; j < p; j++) {
                            System.out.print(MatrixC[i][j] + " ");
                        }
                        System.out.println();
                    }
                    System.out.println("Усі завдання завершили роботу.");
                    break;

                    default:
                    System.out.println("Невірний вибір, спробуйте ще раз.");
            }
            System.out.println();
        }
        scanner.close();
    }
}