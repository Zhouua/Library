import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.ApiResult;
import queries.BorrowHistories;
import queries.CardList;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import java.io.*;
import java.net.InetSocketAddress;
import java.sql.SQLOutput;
import java.util.List;
import java.util.logging.Logger;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());
    private static LibraryManagementSystem library;
    private static DatabaseConnector connector;

    public static void main(String[] args) {


        try {
            // parse connection config from "resources/application.yaml"
            ConnectConfig conf = new ConnectConfig();
            log.info("Success to parse connect config. " + conf.toString());
            // connect to database
            connector = new DatabaseConnector(conf); // 创建数据库连接器
            boolean connStatus = connector.connect();
            if (!connStatus) {
                log.severe("Failed to connect database.");
                System.exit(1);
            }
            library = new LibraryManagementSystemImpl(connector);
            ;// 连接图书管理系统


            // 创建HTTP服务器，监听指定端口
            // 这里是8000，建议不要80端口，容易和其他的撞
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

            // 添加handler，这里就绑定到/card路由
            // 所以localhost:8000/card是会有handler来处理
            server.createContext("/card", new CardHandler());
            server.createContext("/book", new BookHandler());
            server.createContext("/borrow", new BorrowHandler());

            // 启动服务器
            server.start();

            // 标识一下，这样才知道我的后端启动了
            System.out.println("Server is listening on port 8000");

            // release database connection handler
            if (connector.release()) {
                log.info("Success to release connection.");
            } else {
                log.warning("Failed to release connection.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class CardHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==
            if (requestMethod.equals("OPTIONS")) {
                headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                headers.add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(200, -1);
            } else if (requestMethod.equals("GET")) {
                // 处理GET
                handleGetRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            // 响应头，因为是JSON通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();
            // 构建JSON响应数据，这里简化为字符串

            // TEST: 这里写的一个固定的JSON，实际可以查表获取数据，然后再拼出想要的JSON
            //            String response = "[{\"cardId\": 1, \"name\": \"John Doe\", \"department\": \"Computer Science\", \"type\": \"Student\"}," +
            //                    "{\"cardId\": 2, \"name\": \"Jane Smith\", \"department\": \"Electrical Engineering\", \"type\": \"Faculty\"}]";
            //            outputStream.write(response.getBytes());

            // 写入响应数据,调用showCards,把数据库中所有的卡片显示出来
            boolean connStatus = connector.connect();
            if (!connStatus) {
                log.severe("Failed to connect database in cardGET.");
                System.exit(1);
            }
            ApiResult result = library.showCards();
            CardList resCardList = (CardList) result.payload;
            String response = "[]";
            if (resCardList.getCards().size() != 0) {
                response = "[";
                for (int i = 0; i < resCardList.getCards().size(); i++) {
                    // 这里是拼接JSON字符串
                    int cardId = resCardList.getCards().get(i).getCardId();
                    String name = resCardList.getCards().get(i).getName();
                    String department = resCardList.getCards().get(i).getDepartment();
                    String cardType = resCardList.getCards().get(i).getType().getStr();
                    String temp = "{\"id\": " + cardId + ", \"name\": \"" + name + "\", \"department\": \"" + department + "\", \"type\": \"" + cardType + "\"}";
                    response = response.concat(temp);
                    if (i != resCardList.getCards().size() - 1) {
                        response += ",";
                    }
                }
                response += "]";
            }
            System.out.println(response);
            // 写
            outputStream.write(response.getBytes());
            // 流一定要close！！！小心泄漏
            outputStream.close();
            if (connector.release()) {
                log.info("Success to release connection.");
            } else {
                log.warning("Failed to release connection.");
            }
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }

            // 将读到的数据加入数据库
            String contents = requestBodyBuilder.toString();
            System.out.println(contents);
            if (contents.contains("id") && !contents.contains("name")) {
                // 这里是删除操作
                System.out.println("这是删除");
                int cardId = Integer.parseInt(contents.split("\"id\":")[1].replace("}", "").trim());
                System.out.println(cardId);
                boolean connStatus = connector.connect();
                if (!connStatus) {
                    log.severe("Failed to connect database in cardPOST");
                    System.exit(1);
                }
                library.removeCard(cardId);
            } else {
                System.out.println("这是POST");
                String name = contents.split("\"name\":\"")[1].split("\"")[0];
                String department = contents.split("\"department\":\"")[1].split("\"")[0];
                String type = contents.split("\"type\":\"")[1].split("\"")[0];
                if (type.equals("学生")) {
                    type = "S";
                } else {
                    type = "T";
                }
                Card.CardType cardType = Card.CardType.values(type);
                Card card = new Card();
                card.setName(name);
                card.setDepartment(department);
                card.setType(cardType);
                System.out.println(card.toString());
                boolean connStatus = connector.connect();
                if (!connStatus) {
                    log.severe("Failed to connect database in cardPOST");
                    System.exit(1);
                }
                if (!contents.contains("id")) {
                    // register
                    library.registerCard(card);
                } else {
                    // update
                    int cardId = Integer.parseInt(contents.split("\"id\":")[1].split(",")[0]);
                    card.setCardId(cardId);
                    library.updateCard(card);
                }
            }
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);

            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Card created successfully".getBytes());
            outputStream.close();
            if (connector.release()) {
                log.info("Success to release connection.");
            } else {
                log.warning("Failed to release connection.");
            }
        }

    }

    static class BorrowHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==
            if (requestMethod.equals("OPTIONS")) {
                headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                headers.add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(200, -1);
            } else if (requestMethod.equals("GET")) {
                // 处理GET
                handleGetRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            // 响应头，因为是JSON通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();
            // 获取cardId参数
            String content = exchange.getRequestURI().getQuery();
            int cardId = Integer.parseInt(content.split("=")[1]);
            // 查询数据库获取借书记录
            boolean connStatus = connector.connect();
            if (!connStatus) {
                log.severe("Failed to connect database in BookGET");
                System.exit(1);
            }
            ApiResult result = library.showBorrowHistory(cardId); // 传入cardId
            BorrowHistories histories = (BorrowHistories) result.payload;
            List<BorrowHistories.Item> items = histories.getItems();
            // 构建JSON响应数据
            String response = "[";
            for (BorrowHistories.Item item : items) {
                int bookId = item.getBookId();
                Long borrowTime = item.getBorrowTime();
                Long returnTime = item.getReturnTime();
                String temp = "{" + "\"cardID\": " + cardId + ", \"bookID\": " + bookId + ", \"borrowTime\": " + borrowTime + ", \"returnTime\": " + returnTime + "}";
                System.out.println(temp);
                response = response.concat(temp);
                if (items.indexOf(item) != items.size() - 1) {
                    response += ",";
                }
            }
            response += "]";
            System.out.println(response);
            // 写
            outputStream.write(response.getBytes());
            outputStream.close();
            if (connector.release()) {
                log.info("Success to release connection.");
            } else {
                log.warning("Failed to release connection.");
            }

        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {

        }
    }

    static class BookHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==
            if (requestMethod.equals("OPTIONS")) {
                headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                headers.add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(200, -1);
            } else if (requestMethod.equals("GET")) {
                // 处理GET
                handleGetRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            // 响应头，因为是JSON通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();
            // 构建JSON响应数据，这里简化为字符串

            // 写入响应数据,调用showCards,把数据库中所有的卡片显示出来
            boolean connStatus = connector.connect();
            if (!connStatus) {
                log.severe("Failed to connect database in BookGET");
                System.exit(1);
            }
            ApiResult result = library.showBooks();
            List<Book> books = (List<Book>) result.payload;
            String response = "[";
            for (int i = 0; i < books.size(); i++) {
                // 这里是拼接JSON字符串
                int bookId = books.get(i).getBookId();
                String category = books.get(i).getCategory();
                String title = books.get(i).getTitle();
                String press = books.get(i).getPress();
                int publishYear = books.get(i).getPublishYear();
                String author = books.get(i).getAuthor();
                double price = books.get(i).getPrice();
                int stock = books.get(i).getStock();
                String temp = "{\"id\": " + bookId + ", \"category\": \"" + category + "\", \"title\": \"" + title + "\", \"press\": \"" + press + "\", \"publishYear\": " + publishYear + ", \"author\": \"" + author + "\", \"price\": " + price + ", \"stock\": " + stock + "}";
                response = response.concat(temp);
                if (i != books.size() - 1) {
                    response += ",";
                }
            }
            response += "]";
            // 写
            outputStream.write(response.getBytes());
            // 流一定要close！！！小心泄漏
            outputStream.close();

            if (connector.release()) {
                log.info("Success to release connection.");
            } else {
                log.warning("Failed to release connection.");
            }
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
            String contents = requestBodyBuilder.toString();
            System.out.println(contents);

            // 根据请求路径区分逻辑
            String path = exchange.getRequestURI().getPath();
            if (path.contains("/borrow")) {
                System.out.println("Borrow");
                // 处理借书逻辑
                int bookId = Integer.parseInt(contents.split("\"book_id\":")[1].split(",")[0].trim());
                int cardId = Integer.parseInt(contents.split("\"card_id\":\"")[1].split("\"")[0].trim());
                int borrowTime = Integer.parseInt(contents.split("\"borrow_time\":\"")[1].split("\"")[0].trim());
                int returnTime = Integer.parseInt(contents.split("\"return_time\":")[1].replace("}", "").trim());
                Borrow borrow = new Borrow();
                borrow.setBookId(bookId);
                borrow.setCardId(cardId);
                borrow.setBorrowTime(borrowTime);
                borrow.setReturnTime(returnTime);
                System.out.println(bookId + cardId + borrowTime + returnTime);
                boolean connStatus = connector.connect();
                if (!connStatus) {
                    log.severe("Failed to connect database in borrowPOST");
                    exchange.sendResponseHeaders(500, 0);
                    exchange.getResponseBody().close();
                    return;
                }
                ApiResult result = library.borrowBook(borrow);

                if (!result.ok) {
                    String errorMessage = result.message;
                    exchange.getResponseHeaders().set("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(400, errorMessage.getBytes().length);
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(errorMessage.getBytes());
                    outputStream.close();
                    if (connector.release()) {
                        log.info("Success to release connection.");
                    } else {
                        log.warning("Failed to release connection.");
                    }
                    return;
                }
            } else if (path.contains("/return")) {
                System.out.println("Return");
                // 处理还书逻辑
                int bookId = Integer.parseInt(contents.split("\"book_id\":")[1].split(",")[0].trim());
                int cardId = Integer.parseInt(contents.split("\"card_id\":\"")[1].split("\"")[0].trim());
                int borrowTime = Integer.parseInt(contents.split("\"borrow_time\":\"")[1].split("\"")[0].trim());
                int returnTime = Integer.parseInt(contents.split("\"return_time\":\"")[1].split("\"}")[0].trim());

                Borrow borrow = new Borrow();
                borrow.setBookId(bookId);
                borrow.setCardId(cardId);
                borrow.setBorrowTime(borrowTime);
                borrow.setReturnTime(returnTime);

                boolean connStatus = connector.connect();
                if (!connStatus) {
                    log.severe("Failed to connect database in borrowPOST");
                    exchange.sendResponseHeaders(500, 0);
                    exchange.getResponseBody().close();
                    return;
                }
                ApiResult result = library.returnBook(borrow);

                if (!result.ok) {
                    String errorMessage = result.message;
                    exchange.getResponseHeaders().set("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(400, errorMessage.getBytes().length);
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(errorMessage.getBytes());
                    outputStream.close();
                    if (connector.release()) {
                        log.info("Success to release connection.");
                    } else {
                        log.warning("Failed to release connection.");
                    }
                    return;
                }
            } else if (path.contains("/delete")) {
                System.out.println("Delete");
                // 处理删除逻辑
                int bookId = Integer.parseInt(contents.split("\"bookId\":")[1].split("}")[0].trim());
                System.out.println(bookId);
                boolean connStatus = connector.connect();
                if (!connStatus) {
                    log.severe("Failed to connect database in borrowPOST");
                    exchange.sendResponseHeaders(500, 0);
                    exchange.getResponseBody().close();
                    return;
                }
                ApiResult result = library.removeBook(bookId);
                if (!result.ok) {
                    String errorMessage = result.message;
                    exchange.getResponseHeaders().set("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(400, errorMessage.getBytes().length);
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(errorMessage.getBytes());
                    outputStream.close();
                    if (connector.release()) {
                        log.info("Success to release connection.");
                    } else {
                        log.warning("Failed to release connection.");
                    }
                    return;
                }
            } else if (path.contains("increase-stock")) {
                System.out.println("Increase Stock");
                // 处理增加库存逻辑
                int bookId = Integer.parseInt(contents.split("\"bookId\":")[1].split(",")[0].trim());
                int stock = Integer.parseInt(contents.split("\"amount\":")[1].split("\"")[1].trim());
                boolean connStatus = connector.connect();
                if (!connStatus) {
                    log.severe("Failed to connect database in BookIncrease");
                    System.exit(1);
                }
                ApiResult result = library.incBookStock(bookId, stock);
                if (!result.ok) {
                    String errorMessage = result.message;
                    exchange.getResponseHeaders().set("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(400, errorMessage.getBytes().length);
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(errorMessage.getBytes());
                    outputStream.close();
                    if (connector.release()) {
                        log.info("Success to release connection.");
                    } else {
                        log.warning("Failed to release connection.");
                    }
                    return;
                }
            } else if (path.contains("/edit")) {
                System.out.println("Edit Book");
                // 处理编辑图书逻辑
                int bookId = Integer.parseInt(contents.split("\"id\":")[1].split(",")[0].trim());
                String category = contents.split("\"category\":\"")[1].split("\",")[0];
                String title = contents.split("\"title\":\"")[1].split("\",")[0];
                String press = contents.split("\"press\":\"")[1].split("\",")[0];
                int publishYear = Integer.parseInt(contents.split("\"publishYear\":")[1].split(",")[0].trim());
                String author = contents.split("\"author\":\"")[1].split("\",")[0];
                double price = Double.parseDouble(contents.split("\"price\":")[1].split(",")[0].trim());
                int stock = Integer.parseInt(contents.split("\"stock\":")[1].split("}")[0].trim());

                Book book = new Book();
                book.setBookId(bookId);
                book.setCategory(category);
                book.setTitle(title);
                book.setPress(press);
                book.setPublishYear(publishYear);
                book.setAuthor(author);
                book.setPrice(price);
                book.setStock(stock);

                boolean connStatus = connector.connect();
                if (!connStatus) {
                    log.severe("Failed to connect database in BookGET");
                    System.exit(1);
                }
                library.modifyBookInfo(book); // 更新图书
            } else if (path.contains("/batch-import")) {
                System.out.println("Batch Import");
                // 处理批量入库逻辑
                String filePath = contents.split("\"filePath\":\"")[1].split("\"")[0];
                System.out.println("File Path: " + filePath);

                boolean connStatus = connector.connect();
                if (!connStatus) {
                    log.severe("Failed to connect database in BatchImport");
                    exchange.sendResponseHeaders(500, 0);
                    exchange.getResponseBody().close();
                    return;
                }
                try {
                    // 假设文件是 CSV 格式，解析文件内容
                    BufferedReader fileReader = new BufferedReader(new FileReader(filePath));
                    String fileLine;
                    while ((fileLine = fileReader.readLine()) != null) {
                        String[] bookData = fileLine.split(","); // 假设 CSV 文件以逗号分隔
                        Book book = new Book();
                        book.setCategory(bookData[0].trim());
                        book.setTitle(bookData[1].trim());
                        book.setPress(bookData[2].trim());
                        book.setPublishYear(Integer.parseInt(bookData[3].trim()));
                        book.setAuthor(bookData[4].trim());
                        book.setPrice(Double.parseDouble(bookData[5].trim()));
                        book.setStock(Integer.parseInt(bookData[6].trim()));
                        // 将图书存储到数据库
                        library.storeBook(book);
                    }
                    fileReader.close();
                } catch (Exception e) {
                    log.severe("Failed to process batch import: " + e.getMessage());
                    exchange.sendResponseHeaders(500, 0);
                    exchange.getResponseBody().close();
                    return;
                }
            } else {
                System.out.println("Book");
                String category = contents.split("\"category\":\"")[1].split("\",")[0];
                String title = contents.split("\"title\":\"")[1].split("\",")[0];
                String press = contents.split("\"press\":\"")[1].split("\",")[0];
                int publishYear = Integer.parseInt(contents.split("\"publishYear\":\"")[1].split("\",")[0]);
                String author = contents.split("\"author\":\"")[1].split("\",")[0];
                double price = Double.parseDouble(contents.split("\"price\":\"")[1].split("\",")[0]);
                int stock = Integer.parseInt(contents.split("\"stock\":\"")[1].split("\"")[0]);

                Book book = new Book();
                book.setCategory(category);
                book.setTitle(title);
                book.setPress(press);
                book.setPublishYear(publishYear);
                book.setAuthor(author);
                book.setPrice(price);
                book.setStock(stock);
                boolean connStatus = connector.connect();
                if (!connStatus) {
                    log.severe("Failed to connect database in BookGET");
                    System.exit(1);
                }
                library.storeBook(book); // 存入图书
            }


            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);

            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Card created successfully".getBytes());
            outputStream.close();
            if (connector.release()) {
                log.info("Success to release connection.");
            } else {
                log.warning("Failed to release connection.");
            }

        }
    }
}




