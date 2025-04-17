import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private final DatabaseConnector connector;

    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }


    /**
     * register a book to database
     */
    @Override
    public ApiResult storeBook(Book book) {
        Connection connection = connector.getConn();
        try {
            // 设置数据库连接的事务隔离
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            // book_id 是由数据库自动得到
            String category = book.getCategory(); // 得到category
            String title = book.getTitle(); // 得到title
            String press = book.getPress(); // 得到press
            int publishYear = book.getPublishYear(); // 得到publishYear
            String author = book.getAuthor(); // 得到author
            double price = book.getPrice(); // 得到price
            int stock = book.getStock(); // 得到stock
            // checkEqual 用来查看数据库中是否已经有对应的书籍
            String checkEqual = "SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_Year = ? AND author = ?";
            // 使用预编译语句pStmtCheckEqual
            PreparedStatement pStmtCheckEqual = connection.prepareStatement(checkEqual);
            // 传入参数
            pStmtCheckEqual.setString(1, category);
            pStmtCheckEqual.setString(2, title);
            pStmtCheckEqual.setString(3, press);
            pStmtCheckEqual.setInt(4, publishYear);
            pStmtCheckEqual.setString(5, author);
            // 执行查询后返回rsCheckEqual
            ResultSet rsCheckEqual = pStmtCheckEqual.executeQuery();
            // 如果rsCheckEqual非空，则有相同书籍
            if (rsCheckEqual.next()) {
                throw new SQLException("Book already exists");
            }
            // insert 用来插入书籍
            String insert = "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES(?,?,?,?,?,?,?)";
            // 使用预编译语句pStmtInsert
            PreparedStatement pStmtInsert = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
            // 传入参数
            pStmtInsert.setString(1, category);
            pStmtInsert.setString(2, title);
            pStmtInsert.setString(3, press);
            pStmtInsert.setInt(4, publishYear);
            pStmtInsert.setString(5, author);
            pStmtInsert.setDouble(6, price);
            pStmtInsert.setInt(7, stock);
            // 执行插入语句，返回受影响的行数result
            int result = pStmtInsert.executeUpdate();
            if (result != 1) {
                // result 不等于1，说明插入语句出现错误
                throw new SQLException("Failed to store book");
            }
            // 使用commit来递交
            commit(connection);
            // 得到数据库自增的book_id
            ResultSet rsInsert = pStmtInsert.getGeneratedKeys();
            if (rsInsert.next()) {
                int bookId = rsInsert.getInt(1);
                book.setBookId(bookId); // 设置bookId
            } else {
                throw new SQLException("Failed to store book");
            }
        } catch (Exception e) {
            rollback(connection);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully store a book");
    }

    /**
     * increase the book's inventory by bookId & deltaStock.
     */
    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        Connection connection = connector.getConn();
        try {
            // 设置数据库连接的事务隔离
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            // getBook 查询bookId对应的书籍
            String getBook = "SELECT * FROM book WHERE book_id = ?";
            // 使用预编译语句pStmtGetBook
            PreparedStatement pStmtGetBook = connection.prepareStatement(getBook);
            // 传入参数
            pStmtGetBook.setInt(1, bookId);
            // 执行查询后返回rsGetBook
            ResultSet rsGetBook = pStmtGetBook.executeQuery();
            // 不存在对应书籍
            if (!rsGetBook.next()) {
                throw new SQLException("Book does not exist");
            }
            int stock = rsGetBook.getInt("stock");
            // 新库存量小于0
            if (stock + deltaStock < 0) {
                throw new SQLException("Stock limit exceeded");
            }
            // updateBookStock 更新bookId的书籍的库存量
            String updateBookStock = "UPDATE book SET stock = ? WHERE book_id = ?";
            // 使用预编译语句pStmtUpdateBookStock
            PreparedStatement pStmtUpdateBookStock = connection.prepareStatement(updateBookStock);
            pStmtUpdateBookStock.setInt(1, stock + deltaStock);
            pStmtUpdateBookStock.setInt(2, bookId);
            // 更新结果为result
            int result = pStmtUpdateBookStock.executeUpdate();
            if (result != 1) {
                throw new SQLException("Failed to store book");
            }
            // 使用commit递交
            commit(connection);
        } catch (Exception e) {
            rollback(connection);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully increase the book's stock");
    }

    /**
     * batch store books.
     */
    @Override
    public ApiResult storeBook(List<Book> books) {
        Connection connection = connector.getConn();
        try {
            // 将每本书逐一插入，有一本书插入失败则全部回滚
            for (Book book : books) {
                // book_id 是由数据库自动得到
                String category = book.getCategory(); // 得到category
                String title = book.getTitle(); // 得到title
                String press = book.getPress(); // 得到press
                int publishYear = book.getPublishYear(); // 得到publishYear
                String author = book.getAuthor(); // 得到author
                double price = book.getPrice(); // 得到price
                int stock = book.getStock(); // 得到stock
                // checkEqual 用来查看数据库中是否已经有对应的书籍
                String checkEqual = "SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_Year = ? AND author = ?";
                // 使用预编译语句pStmtCheckEqual
                PreparedStatement pStmtCheckEqual = connection.prepareStatement(checkEqual);
                // 传入参数
                pStmtCheckEqual.setString(1, category);
                pStmtCheckEqual.setString(2, title);
                pStmtCheckEqual.setString(3, press);
                pStmtCheckEqual.setInt(4, publishYear);
                pStmtCheckEqual.setString(5, author);
                // 执行查询后返回rsCheckEqual
                ResultSet rsCheckEqual = pStmtCheckEqual.executeQuery();
                // 如果rsCheckEqual非空，则有相同书籍
                if (rsCheckEqual.next()) {
                    throw new SQLException("Book already exists");
                }
                // insert 用来插入书籍
                String insert = "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES(?,?,?,?,?,?,?)";
                // 使用预编译语句pStmtInsert
                PreparedStatement pStmtInsert = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
                // 传入参数
                pStmtInsert.setString(1, category);
                pStmtInsert.setString(2, title);
                pStmtInsert.setString(3, press);
                pStmtInsert.setInt(4, publishYear);
                pStmtInsert.setString(5, author);
                pStmtInsert.setDouble(6, price);
                pStmtInsert.setInt(7, stock);
                // 执行插入语句，返回受影响的行数result
                int result = pStmtInsert.executeUpdate();
                if (result != 1) {
                    // result 不等于1，说明插入语句出现错误
                    throw new SQLException("Failed to store book");
                }
                // 得到数据库自增的book_id
                ResultSet rsInsert = pStmtInsert.getGeneratedKeys();
                if (rsInsert.next()) {
                    int bookId = rsInsert.getInt(1);
                    book.setBookId(bookId); // 设置bookId
                } else {
                    throw new SQLException("Failed to store book");
                }
            }
            // 使用commit递交
            commit(connection);
        } catch (Exception e) {
            rollback(connection);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully store a list of books");
    }

    /**
     * remove this book from library system.
     */
    @Override
    public ApiResult removeBook(int bookId) {
        Connection connection = connector.getConn();
        try {
            // getBook 查看书库中是否有对应bookId的书籍
            String getBook = "SELECT * FROM book WHERE book_id = ?";
            PreparedStatement pStmtGetBook = connection.prepareStatement(getBook);
            pStmtGetBook.setInt(1, bookId);
            ResultSet rsGetBook = pStmtGetBook.executeQuery();
            // 不存在对应书籍
            if (!rsGetBook.next()) {
                throw new SQLException("Book does not exist");
            }
            // bookNotReturned 查看是否bookId对应的书没有归还
            String bookNotReturned = "SELECT * FROM borrow WHERE book_id = ? AND return_time = 0";
            PreparedStatement pStmtBookNotReturned = connection.prepareStatement(bookNotReturned);
            pStmtBookNotReturned.setInt(1, bookId);
            ResultSet rsBookNotReturned = pStmtBookNotReturned.executeQuery();
            // bookId的书没有被归还
            if (rsBookNotReturned.next()) {
                throw new SQLException("Book is not returned");
            }
            // removeBook 删除对应书籍
            String removeBook = "DELETE FROM book WHERE book_id = ?";
            PreparedStatement pStmtRemoveBook = connection.prepareStatement(removeBook);
            pStmtRemoveBook.setInt(1, bookId);
            int result = pStmtRemoveBook.executeUpdate();
            // result 不为1说明删除失败
            if (result != 1) {
                throw new SQLException("Failed to remove book");
            }
            // 使用commit递交
            commit(connection);
        } catch (Exception e) {
            rollback(connection);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully remove book");
    }

    /**
     * modify a book's information by book_id.
     */
    @Override
    public ApiResult modifyBookInfo(Book book) {
        Connection connection = connector.getConn();
        try {
            // getBook 查询对应的书籍
            String getBook = "SELECT * FROM book WHERE book_id = ?";
            PreparedStatement pStmtGetBook = connection.prepareStatement(getBook);
            pStmtGetBook.setInt(1, book.getBookId());
            ResultSet rsGetBook = pStmtGetBook.executeQuery();
            // 系统中没有对应书籍
            if (!rsGetBook.next()) {
                throw new SQLException("Book does not exist");
            }
            // 得到book的各个信息
            String category = book.getCategory();
            String title = book.getTitle();
            String press = book.getPress();
            int publishYear = book.getPublishYear();
            String author = book.getAuthor();
            double price = book.getPrice();
            // updateBook 更新对应书籍的信息
            String updateBook = "UPDATE book SET category = ?, title = ?, press = ?, publish_year = ?, author = ?, price = ? WHERE book_id = ?";
            PreparedStatement pStmtUpdateBook = connection.prepareStatement(updateBook);
            pStmtUpdateBook.setString(1, category);
            pStmtUpdateBook.setString(2, title);
            pStmtUpdateBook.setString(3, press);
            pStmtUpdateBook.setInt(4, publishYear);
            pStmtUpdateBook.setString(5, author);
            pStmtUpdateBook.setDouble(6, price);
            pStmtUpdateBook.setInt(7, book.getBookId());
            int result = pStmtUpdateBook.executeUpdate();
            // 若result不等于1，说明update失败
            if (result != 1) {
                throw new SQLException("Failed to update book");
            }
            // 使用commit递交
            commit(connection);
        } catch (Exception e) {
            rollback(connection);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully modify book's info");
    }

    /**
     * query books according to different query conditions.
     */
    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        Connection connection = connector.getConn();
        try {
            String category = conditions.getCategory(); // 类别点查（精确查询）
            String title = conditions.getTitle(); // 书名点查（模糊查询）
            String press = conditions.getPress(); // 出版社点查（模糊查询）
            Integer minPublishYear = conditions.getMinPublishYear(); // 年份范围
            Integer maxPublishYear = conditions.getMaxPublishYear();
            String author = conditions.getAuthor(); // 作者点查（模糊查询）
            Double minPrice = conditions.getMinPrice(); // 价格范围
            Double maxPrice = conditions.getMaxPrice();
            // queryBook进行查询对应书籍
            // 添加对应的条件，使用 = 来精确查询，使用 LIKE 来模糊查询，当为null时，使用True来占位
            String queryBook = "SELECT * FROM book WHERE " + (conditions.getCategory() == null ? "True" : "category = ?") +
                    " AND " + (conditions.getTitle() == null ? "True" : "title LIKE ?") +
                    " AND " + (conditions.getPress() == null ? "True" : "press LIKE ?") +
                    " AND " + (conditions.getAuthor() == null ? "True" : "author LIKE ?") +
                    " AND " + (conditions.getMinPublishYear() == null ? "True" : "publish_year >= ?") +
                    " AND " + (conditions.getMaxPublishYear() == null ? "True" : "publish_year <= ?") +
                    " AND " + (conditions.getMinPrice() == null ? "True" : "price >= ?") +
                    " AND " + (conditions.getMaxPrice() == null ? "True" : "price <= ?")
                    + " ORDER BY " + conditions.getSortBy() + " " + conditions.getSortOrder() + ", book_id ASC"; // 最后确保排序的顺序
            PreparedStatement pStmtQueryBook = connection.prepareStatement(queryBook);
            // 传入参数，用index表示传入第几个参数
            int index = 1;
            if (category != null) {
                pStmtQueryBook.setString(index++, category);
            } // 精确查询
            if (title != null) {
                pStmtQueryBook.setString(index++, "%" + title + "%");
            } // 模糊查询
            if (press != null) {
                pStmtQueryBook.setString(index++, "%" + press + "%");
            } // 模糊查询
            if (author != null) {
                pStmtQueryBook.setString(index++, "%" + author + "%");
            } // 模糊查询
            if (minPublishYear != null) {
                pStmtQueryBook.setInt(index++, minPublishYear);
            }
            if (maxPublishYear != null) {
                pStmtQueryBook.setInt(index++, maxPublishYear);
            }
            if (minPrice != null) {
                pStmtQueryBook.setDouble(index++, minPrice);
            }
            if (maxPrice != null) {
                pStmtQueryBook.setDouble(index++, maxPrice);
            }
            // 执行对应的查询
            ResultSet rsQueryBook = pStmtQueryBook.executeQuery();
            // 将得到的书籍用Book形式存储
            List<Book> books = new ArrayList<>();
            while (rsQueryBook.next()) {
                Book book = new Book();
                book.setBookId(rsQueryBook.getInt("book_id"));
                book.setCategory(rsQueryBook.getString("category"));
                book.setTitle(rsQueryBook.getString("title"));
                book.setPress(rsQueryBook.getString("press"));
                book.setPublishYear(rsQueryBook.getInt("publish_year"));
                book.setAuthor(rsQueryBook.getString("author"));
                book.setPrice(rsQueryBook.getDouble("price"));
                book.setStock(rsQueryBook.getInt("stock"));
                books.add(book);
            }
            // 用commit递交
            commit(connection);
            BookQueryResults bookQueryResults = new BookQueryResults(books);
            return new ApiResult(true, "Successfully query book", bookQueryResults);
        } catch (Exception e) {
            rollback(connection);
            return new ApiResult(false, e.getMessage());
        }
    }

    /**
     * a user borrows one book with the specific card.
     */
    @Override
    public ApiResult borrowBook(Borrow borrow) {
        Connection connection = connector.getConn();
        try {
            // 设置数据库连接的事务隔离
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            // checkBorrow 检查是否该卡之前借过这本书但未归还
            String checkBorrow = "SELECT * FROM borrow WHERE card_id = ? AND book_id = ? AND return_time = 0";
            PreparedStatement pStmtCheckBorrow = connection.prepareStatement(checkBorrow);
            pStmtCheckBorrow.setInt(1, borrow.getCardId());
            pStmtCheckBorrow.setInt(2, borrow.getBookId());
            ResultSet rsCheckBorrow = pStmtCheckBorrow.executeQuery();
            // 借过这本书但未归还
            if (rsCheckBorrow.next()) {
                throw new SQLException("There's already a borrowed book not returned");
            }
            // 需要将库存量-1，并且判断是否能够库存量-1，如果不能则回滚
            if (incBookStock(borrow.getBookId(), -1).ok == false) {
                throw new SQLException("No more stock for the book");
            }
            ;
            // borrowBook 执行插入一条借书
            String borrowBook = "INSERT INTO borrow(card_id, book_id, borrow_time) VALUES(?, ?, ?)";
            PreparedStatement pStmtBorrow = connection.prepareStatement(borrowBook);
            pStmtBorrow.setInt(1, borrow.getCardId());
            pStmtBorrow.setInt(2, borrow.getBookId());
            pStmtBorrow.setLong(3, borrow.getBorrowTime());
            int result = pStmtBorrow.executeUpdate();
            // result 不等于1则说明插入失败
            if (result != 1) {
                throw new SQLException("Failed to borrow book");
            }
            // 用commit递交
            commit(connection);
        } catch (Exception e) {
            rollback(connection);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully borrow book");
    }

    /**
     * A user return one book with specific card.
     */
    @Override
    public ApiResult returnBook(Borrow borrow) {
        Connection connection = connector.getConn();
        try {
            // checkBorrow 检查是否有对应卡借的书
            String checkBorrow = "SELECT * FROM borrow WHERE card_id = ? AND book_id = ? AND borrow_time = ?";
            PreparedStatement pStmtCheckBorrow = connection.prepareStatement(checkBorrow);
            pStmtCheckBorrow.setInt(1, borrow.getCardId());
            pStmtCheckBorrow.setInt(2, borrow.getBookId());
            pStmtCheckBorrow.setLong(3, borrow.getBorrowTime());
            ResultSet rsCheckBorrow = pStmtCheckBorrow.executeQuery();
            // 说明这张卡没有借对应的书
            if (!rsCheckBorrow.next()) {
                throw new SQLException("No such card borrow the according book");
            }
            // returnBook 表示还书并更新returnTime
            String returnBook = "UPDATE borrow SET return_time = ? WHERE card_id = ? AND book_id = ? AND borrow_time = ?";
            PreparedStatement pStmtReturnBook = connection.prepareStatement(returnBook);
            pStmtReturnBook.setLong(1, borrow.getReturnTime());
            pStmtReturnBook.setInt(2, borrow.getCardId());
            pStmtReturnBook.setInt(3, borrow.getBookId());
            pStmtReturnBook.setLong(4, borrow.getBorrowTime());
            int result = pStmtReturnBook.executeUpdate();
            // result 不等于1说明更新失败
            if (result != 1) {
                throw new SQLException("Failed to return book");
            }
            // 库存量 + 1
            incBookStock(borrow.getBookId(), 1);
            // 用commit递交
            commit(connection);
        } catch (Exception e) {
            rollback(connection);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully return book");
    }

    /**
     * list all borrow histories for a specific card.
     */
    @Override
    public ApiResult showBorrowHistory(int cardId) {
        Connection connection = connector.getConn();
        try {
            // cardBorrow 查询对应cardId借的书，按照借书时间递减、书号递增的方式排序
            String cardBorrow = "SELECT * FROM borrow NATURAL JOIN book WHERE card_id = ? ORDER BY borrow_time DESC, book_id ASC";
            PreparedStatement pStmtCheckBorrow = connection.prepareStatement(cardBorrow);
            pStmtCheckBorrow.setInt(1, cardId);
            ResultSet rsCardBorrow = pStmtCheckBorrow.executeQuery();
            // 将所有查询到的item加入histories当中
            List<BorrowHistories.Item> histories = new ArrayList<>();
            while (rsCardBorrow.next()) {
                BorrowHistories.Item item = new BorrowHistories.Item();
                item.setCardId(rsCardBorrow.getInt("card_id"));
                item.setBookId(rsCardBorrow.getInt("book_id"));
                item.setCategory(rsCardBorrow.getString("category"));
                item.setTitle(rsCardBorrow.getString("title"));
                item.setPress(rsCardBorrow.getString("press"));
                item.setPublishYear(rsCardBorrow.getInt("publish_year"));
                item.setAuthor(rsCardBorrow.getString("author"));
                item.setPrice(rsCardBorrow.getDouble("price"));
                item.setBorrowTime(rsCardBorrow.getLong("borrow_time"));
                item.setReturnTime(rsCardBorrow.getLong("return_time"));
                histories.add(item);
            }
            // 用commit递交
            commit(connection);
            BorrowHistories borrowHistories = new BorrowHistories(histories);
            return new ApiResult(true, "Successfully show borrow history", borrowHistories);
        } catch (Exception e) {
            rollback(connection);
            return new ApiResult(false, e.getMessage());
        }
    }

    /**
     * create a new borrow card.
     */
    @Override
    public ApiResult registerCard(Card card) {
        Connection connection = connector.getConn();
        try {
            String name = card.getName();
            String department = card.getDepartment();
            String cardType = card.getType().getStr();
            // checkCard 检查是否已经有相同的card
            String checkCard = "SELECT * FROM card WHERE name = ? AND department = ? AND type = ?";
            PreparedStatement pStmtCheckCard = connection.prepareStatement(checkCard);
            pStmtCheckCard.setString(1, name);
            pStmtCheckCard.setString(2, department);
            pStmtCheckCard.setString(3, cardType);
            ResultSet rsCard = pStmtCheckCard.executeQuery();
            // rsCard非空说明已经有相同的card
            if (rsCard.next()) {
                throw new SQLException("Card already exists");
            }
            // registerCard 用来插入一条新card，注意card_id是自增的
            String registerCard = "INSERT INTO card(name, department, type) VALUES(?, ?, ?)";
            PreparedStatement pStmtRegisterCard = connection.prepareStatement(registerCard, Statement.RETURN_GENERATED_KEYS);
            pStmtRegisterCard.setString(1, name);
            pStmtRegisterCard.setString(2, department);
            pStmtRegisterCard.setString(3, cardType);
            int result = pStmtRegisterCard.executeUpdate();
            // result 不等于1说明插入失败
            if (result != 1) {
                throw new SQLException("Failed to register card");
            }
            // 用commit递交
            commit(connection);
            // 得到自增的cardId
            ResultSet rsInsert = pStmtRegisterCard.getGeneratedKeys();
            if (rsInsert.next()) {
                card.setCardId(rsInsert.getInt(1));
            }
        } catch (Exception e) {
            rollback(connection);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully register a new card");
    }

    /**
     * simply remove a card.
     */
    @Override
    public ApiResult removeCard(int cardId) {
        Connection connection = connector.getConn();
        try {
            // checkCard 检查是否有cardId的card
            String checkCard = "SELECT * FROM card WHERE card_id = ?";
            PreparedStatement pStmtCheckCard = connection.prepareStatement(checkCard);
            pStmtCheckCard.setInt(1, cardId);
            ResultSet rsCard = pStmtCheckCard.executeQuery();
            // 没有对应的card
            if (!rsCard.next()) {
                throw new SQLException("Card does not exist");
            }
            // checkNotReturned 检查是否有书未还
            String checkNotReturned = "SELECT * FROM borrow WHERE card_id = ? AND borrow.return_time = 0";
            PreparedStatement pStmtCheckNotReturned = connection.prepareStatement(checkNotReturned);
            pStmtCheckNotReturned.setInt(1, cardId);
            ResultSet rsNotReturned = pStmtCheckNotReturned.executeQuery();
            // 非空说明还有书未还，无法删除
            if (rsNotReturned.next()) {
                throw new SQLException("There's a book not returned with this card");
            }
            // removeCard 删除对应card
            String removeCard = "DELETE FROM card WHERE card_id = ?";
            PreparedStatement pStmtRemoveCard = connection.prepareStatement(removeCard);
            pStmtRemoveCard.setInt(1, cardId);
            int result = pStmtRemoveCard.executeUpdate();
            // result 不等于1说明删除失败
            if (result != 1) {
                throw new SQLException("Failed to remove card");
            }
            // 用commit递交
            commit(connection);
        } catch (Exception e) {
            rollback(connection);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully remove a card");
    }

    /**
     * show all books order by book_id.
     */
    public ApiResult showBooks() {
        Connection connection = connector.getConn();
        try {
            // listBooks 查询所有book并以bookId生序
            String listBooks = "SELECT * FROM book ORDER BY book_id ASC";
            PreparedStatement pStmtShowBooks = connection.prepareStatement(listBooks);
            ResultSet rsShowBooks = pStmtShowBooks.executeQuery();
            // books 存储所有对应book
            List<Book> books = new ArrayList<>();
            while (rsShowBooks.next()) {
                Book book = new Book();
                book.setBookId(rsShowBooks.getInt("book_id"));
                book.setCategory(rsShowBooks.getString("category"));
                book.setTitle(rsShowBooks.getString("title"));
                book.setPress(rsShowBooks.getString("press"));
                book.setPublishYear(rsShowBooks.getInt("publish_year"));
                book.setAuthor(rsShowBooks.getString("author"));
                book.setPrice(rsShowBooks.getDouble("price"));
                book.setStock(rsShowBooks.getInt("stock"));
                books.add(book);
            }
            // 用commit递交
            commit(connection);
            return new ApiResult(true, "Successfully show books", books);
        } catch (Exception e) {
            rollback(connection);
            return new ApiResult(false, e.getMessage());
        }
    }

    /**
     * list all cards order by card_id.
     */
    @Override
    public ApiResult showCards() {
        Connection connection = connector.getConn();
        try {
            // listCards 查询所有card并以cardId生序
            String listCards = "SELECT * FROM card ORDER BY card_id ASC";
            PreparedStatement pStmtShowCards = connection.prepareStatement(listCards);
            ResultSet rsShowCards = pStmtShowCards.executeQuery();
            // cards 存储所有对应card
            List<Card> cards = new ArrayList<>();
            while (rsShowCards.next()) {
                Card card = new Card();
                card.setCardId(rsShowCards.getInt("card_id"));
                card.setName(rsShowCards.getString("name"));
                card.setDepartment(rsShowCards.getString("department"));
                Card.CardType cardType = Card.CardType.values(rsShowCards.getString("type"));
                card.setType(cardType);
                cards.add(card);
            }
            // 用commit递交
            commit(connection);
            // 创建CardList
            CardList CL = new CardList(cards);
            return new ApiResult(true, "Successfully show cards", CL);
        } catch (Exception e) {
            rollback(connection);
            return new ApiResult(false, e.getMessage());
        }
    }

    /**
     * update card
     */
    public ApiResult updateCard(Card card) {
        // id 不能修改
        Connection connection = connector.getConn();
        try {
            // checkCard 检查是否有对应的id
            String checkCard = "SELECT * FROM card WHERE card_id = ?";
            PreparedStatement pStmtCheckCard = connection.prepareStatement(checkCard);
            pStmtCheckCard.setInt(1, card.getCardId());
            ResultSet rsCard = pStmtCheckCard.executeQuery();
            if (!rsCard.next()) {
                throw new SQLException("Card does not exist");
            }
            // updateCard 执行更新card信息，注意不能修改id
            String updateCard = "UPDATE card SET name = ?, department = ?, type = ? WHERE card_id = ?";
            PreparedStatement pStmtUpdateCard = connection.prepareStatement(updateCard);
            pStmtUpdateCard.setString(1, card.getName());
            pStmtUpdateCard.setString(2, card.getDepartment());
            pStmtUpdateCard.setString(3, card.getType().getStr());
            pStmtUpdateCard.setInt(4, card.getCardId());
            // 执行更新
            int result = pStmtUpdateCard.executeUpdate();
            if (result != 1) {
                throw new SQLException("Failed to update card");
            }
            // 用commit递交
            commit(connection);
        } catch (Exception e) {
            rollback(connection);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Successfully update card");
    }

    /**
     * reset database to its initial state.
     */
    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
