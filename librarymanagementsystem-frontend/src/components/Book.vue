<template>
  <el-scrollbar height="100%" style="width: 100%;">
    <!-- 标题和搜索框 -->
    <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold;">图书管理
      <el-input v-model="toSearch" :prefix-icon="Search"
                placeholder="查询书籍"
                style="width: 15vw; min-width: 150px; margin-left: 30px; margin-right: 30px; float: right;" clearable/>
    </div>

    <!-- 图书列表 -->
    <el-table :data="filteredBooks" style="width: 100%; margin-top: 20px;">
      <el-table-column prop="id" label="图书ID" width="100"/>
      <el-table-column prop="title" label="书名"/>
      <el-table-column prop="category" label="类别"/>
      <el-table-column prop="author" label="作者"/>
      <el-table-column prop="press" label="出版社"/>
      <el-table-column prop="stock" label="当前库存"/>
      <el-table-column label="操作" width="300">
        <template #default="scope">
          <el-button type="primary" icon="Edit" @click="editBook(scope.row)" style="margin: 10px;">编辑</el-button>
          <el-button type="info" icon="Download" @click="borrowBook(scope.row.id)" style="margin: 10px;">借书
          </el-button>
          <el-button type="success" icon="Upload" @click="returnBook(scope.row.id)" style="margin: 10px;">还书
          </el-button>
          <el-button type="warning" icon="Plus" @click="openAddStockDialog(scope.row.id)" style="margin: 10px;">增加库存
          </el-button>
          <el-button type="danger" icon="Delete" @click="deleteBook(scope.row.id)" style="margin: 10px;">删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 借书对话框 -->
    <el-dialog v-model="showBorrowDialog" title="借书">
      <el-form :model="borrowInfo">
        <el-form-item label="借书证ID">
          <el-input v-model="borrowInfo.card_id"/>
        </el-form-item>
        <el-form-item label="借书时间">
          <el-input v-model="borrowInfo.borrow_time"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBorrowDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmBorrow">确定</el-button>
      </template>
    </el-dialog>

    <!-- 还书对话框 -->
    <el-dialog v-model="showReturnDialog" title="还书">
      <el-form :model="returnInfo">
        <el-form-item label="借书证ID">
          <el-input v-model="returnInfo.card_id"/>
        </el-form-item>
        <el-form-item label="借书时间">
          <el-input v-model="returnInfo.borrow_time"/>
        </el-form-item>
        <el-form-item label="还书时间">
          <el-input v-model="returnInfo.return_time"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReturnDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmReturn">确定</el-button>
      </template>
    </el-dialog>

    <!-- 增加库存  -->
    <el-dialog v-model="showAddStockDialog" title="增加库存">
      <el-form :model="addStockInfo">
        <el-form-item label="增加数量">
          <el-input v-model="addStockInfo.amount" type="number"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddStockDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmIncreaseStock">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增图书按钮 -->
    <el-button type="primary" style="margin: 20px;" @click="showAddBookDialog = true">新增图书</el-button>
    <el-button type="success" style="margin: 20px;" @click="batchImportBooks">批量入库</el-button>
    <el-dialog v-model="showBatchImportDialog" title="批量入库">
      <el-form>
        <el-form-item label="文件路径">
          <el-input v-model="batchImportFilePath" placeholder="请输入文件路径"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBatchImportDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmBatchImport">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增图书对话框 -->
    <el-dialog v-model="showAddBookDialog" title="新增图书">
      <el-form :model="newBook">
        <el-form-item label="类别">
          <el-input v-model="newBook.category"/>
        </el-form-item>
        <el-form-item label="书名">
          <el-input v-model="newBook.title"/>
        </el-form-item>
        <el-form-item label="出版社">
          <el-input v-model="newBook.press"/>
        </el-form-item>
        <el-form-item label="年份">
          <el-input v-model="newBook.publishYear"/>
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="newBook.author"/>
        </el-form-item>
        <el-form-item label="价格">
          <el-input v-model="newBook.price"/>
        </el-form-item>
        <el-form-item label="初始库存">
          <el-input v-model="newBook.stock"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddBookDialog = false">取消</el-button>
        <el-button type="primary" @click="addBook">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑图书对话框 -->
    <el-dialog v-model="showEditBookDialog" title="编辑图书">
      <el-form :model="editBookInfo">
        <el-form-item label="类别">
          <el-input v-model="editBookInfo.category"/>
        </el-form-item>
        <el-form-item label="书名">
          <el-input v-model="editBookInfo.title"/>
        </el-form-item>
        <el-form-item label="出版社">
          <el-input v-model="editBookInfo.press"/>
        </el-form-item>
        <el-form-item label="年份">
          <el-input v-model="editBookInfo.publishYear"/>
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="editBookInfo.author"/>
        </el-form-item>
        <el-form-item label="价格">
          <el-input v-model="editBookInfo.price"/>
        </el-form-item>
        <el-form-item label="库存">
          <el-input v-model="editBookInfo.stock"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditBookDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmEditBook">确定</el-button>
      </template>
    </el-dialog>


  </el-scrollbar>
</template>

<script>
import {Search} from '@element-plus/icons-vue';
import {ElMessage} from 'element-plus';
import axios from 'axios';

export default {
  data() {
    return {
      books: [{ // 图书列表
        id: 1,
        category: "计算机",
        title: "深入理解计算机系统",
        press: "浙江大学出版社",
        publishYear: "2018",
        author: "Randal E. Bryant",
        price: 89.00,
        stock: 10
      }, {
        id: 2,
        category: "历史",
        title: "TestBook",
        press: "人民出版社",
        publishYear: "2010",
        author: "张三",
        price: 52.10,
        stock: 3
      }],
      borrowInfo: { // 借书信息
        book_id: null,
        card_id: '',
        borrow_time: '',
        return_time: 0
      },
      returnInfo: { // 还书信息
        book_id: null,
        card_id: '',
        borrow_time: '',
        return_time: ''
      },
      toSearch: '', // 搜索内容
      showAddBookDialog: false, // 新增图书对话框可见性
      showBatchImportDialog: false, // 批量入库对话框可见性
      showDetailsDialog: false, // 详情对话框可见性
      showBorrowDialog: false, // 借书对话框可见性
      showReturnDialog: false, // 还书对话框可见性
      showAddStockDialog: false, // 增加库存对话框可见性
      showEditBookDialog: false, // 编辑图书对话框可见性
      editBookInfo: { // 编辑图书信息
        id: null,
        category: '',
        title: '',
        press: '',
        publishYear: '',
        author: '',
        price: '',
        stock: ''
      },
      addStockInfo: { // 增加库存信息
        book_id: null,
        amount: 0
      },
      newBook: { // 新增图书信息
        category: '',
        title: '',
        press: '',
        publishYear: '',
        author: '',
        price: '',
        stock: ''
      },
      batchImportFilePath: '', // 批量入库文件路径
      Search
    };
  },
  computed: {
    filteredBooks() {
      return this.books.filter(book =>
          book.title.includes(this.toSearch) ||
          book.author.includes(this.toSearch) ||
          book.press.includes(this.toSearch)
      );
    }
  },
  methods: {
    async fetchBooks() {
      try {
        const response = await axios.get('/books');
        this.books = response.data;
      } catch (error) {
        ElMessage.error('获取图书列表失败');
      }
    },
    async addBook() {
      try {
        await axios.post('/books', this.newBook);
        ElMessage.success('新增图书成功');
        this.showAddBookDialog = false;
        this.fetchBooks();
      } catch (error) {
        ElMessage.error('新增图书失败');
      }
    },
    async batchImportBooks() {
      this.showBatchImportDialog = true; // 显示批量入库对话框
    },
    async confirmBatchImport() {
      try {
        // 假设通过文件路径进行批量入库
        await axios.post('/books/batch-import', {filePath: this.batchImportFilePath});
        ElMessage.success('批量入库成功');
        this.showBatchImportDialog = false;
        this.fetchBooks();
      } catch (error) {
        ElMessage.error('批量入库失败');
      }
    },
    async deleteBook(bookId) {
      const confirmed = window.confirm('确定要删除这本书吗？');
      if (!confirmed) {
        return; // 如果用户取消，则不执行删除操作
      }
      try {
        await axios.post(`/books/delete`, {bookId});
        ElMessage.success('删除图书成功');
        this.fetchBooks();
      } catch (error) {
        const errorMessage = error.response?.data || '删除图书失败';
        ElMessage.error(errorMessage);
      }
    },
    async increaseStock(bookId, amount) {
      try {
        const response = await axios.post(`/books/increase-stock`, {bookId, amount});
        ElMessage.success('库存增加成功');
        this.fetchBooks(); // 刷新图书列表
      } catch (error) {
        const errorMessage = error.response?.data || '增加库存失败';
        ElMessage.error(errorMessage); // 显示后端返回的错误消息
      }
    },
    async borrowBook(bookId) {
      this.borrowInfo.book_id = bookId; // 设置书籍ID
      this.borrowInfo.card_id = ''; // 清空cardID
      this.borrowInfo.borrow_time = ''; // 清空借书时间
      this.showBorrowDialog = true; // 显示借书对话框
    },
    async confirmBorrow() {
      try {
        await axios.post(`/books/borrow`, this.borrowInfo); // 发送借书请求
        ElMessage.success('借书成功');
        this.showBorrowDialog = false; // 关闭对话框
        this.fetchBooks(); // 刷新图书列表
      } catch (error) {
        const errorMessage = error.response?.data || '借书失败';
        ElMessage.error(errorMessage); // 显示后端返回的错误消息
      }
    },
    async returnBook(bookId) {
      this.returnInfo.book_id = bookId; // 设置书籍ID
      this.returnInfo.card_id = ''; // 清空cardID
      this.returnInfo.borrow_time = ''; // 清空借书时间
      this.returnInfo.return_time = ''; // 清空还书时间
      this.showReturnDialog = true; // 显示还书对话框
    },
    async confirmReturn() {
      try {
        await axios.post(`/books/return`, this.returnInfo); // 发送还书请求
        ElMessage.success('还书成功');
        this.showReturnDialog = false; // 关闭对话框
        this.fetchBooks(); // 刷新图书列表
      } catch (error) {
        const errorMessage = error.response?.data || '还书失败';
        ElMessage.error(errorMessage); // 显示后端返回的错误消息
      }
    },
    openAddStockDialog(bookId) {
      this.addStockInfo.book_id = bookId;
      this.addStockInfo.amount = 0; // 重置数量
      this.showAddStockDialog = true;
    },
    async confirmIncreaseStock() {
      const {book_id, amount} = this.addStockInfo;
      await this.increaseStock(book_id, amount);
      this.showAddStockDialog = false;
    },
    editBook(book) {
      this.editBookInfo = {...book}; // 复制选中图书信息
      this.showEditBookDialog = true; // 显示编辑图书对话框
    },
    async confirmEditBook() {
      try {
        await axios.post(`/books/edit`, this.editBookInfo); // 发送编辑图书请求
        ElMessage.success('编辑图书成功');
        this.showEditBookDialog = false; // 关闭对话框
        this.fetchBooks(); // 刷新图书列表
      } catch (error) {
        const errorMessage = error.response?.data || '编辑图书失败';
        ElMessage.error(errorMessage); // 显示后端返回的错误消息
      }
    },
  },
  mounted() {
    this.fetchBooks();
  }
};
</script>

<style scoped>
</style>