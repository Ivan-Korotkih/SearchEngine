# ПОИСКОВИК ПО САЙТАМ (SearchEngine)
_ _ _ _ _
### *SearchEngine* - представляет из себя поисковик по сайтам, который ищет на страницах этих сайтов необходимые русские слова или фразы.
_ _ _ _ _
### *Процесс поиска*
1. Перед поиском нужных слов, необходимо произвести индексацию сайтов (осуществить поис русских слов на каждой странице сайтов)
   + Для этого на вкладке MANAGEMENT нажать кнопку START INDEXING ![management](https://drive.google.com/file/d/15dsVdnn-4qy-lm3uH1mLUSzqNGHzbpBV/view?usp=drive_link)
   + Результатом чего во вкладке DASHBOARD появится статистика о проиндексированных сайтах (количество найденых страниц, лемм и ошибках, возникших при индексации) ![DASHBOARD](https://drive.google.com/file/d/1yCvG4k9czKlnzrIar0rp8XsTJW6ZIIQD/view?usp=sharing)
2. Во вкладке SEARCH выбрать сайт (все сайты), по которому будет производится поиcк, в поле *query* ввести поисковый запрос и нажать кнопку SEARCH. Получить результат ![reqwest](https://drive.google.com/file/d/1ec3kXB2z7WPo0HcLom9fnu7Tg8pkRpuR/view?usp=sharing)
   + При нажатии на один из результатов переходим на страницу с необходимыми словами.
3. В дополнении к функционалу программы есть возможность произвести переиндексацию отдельной страницы во вкладке MANAGEMENT, введя в поле Add/update page неоюходимый адрес страницы и нажав на кнопку Add/update
_ _ _ _ _
### *Cтэк используемых технологий*
+ ***SpringBoot***
+ *apache.lucene.morphology*
+ *lombok*
+ *mysql*
+ *jsoup*
+ *imgscalr-lib*
_ _ _ _ _
### *Запуск программы*
+ Скачать SearchEngine-1.0-SNAPSHOT.jar
+ В командной строке:
    + перейти в директорию где хранится файл SearchEngine-1.0-SNAPSHOT.jar. Н-р `C:\Users\Иван> cd C:\Users\Иван\Desktop`
    + выполнить команду: `C:\Users\Иван\Desktop>java -jar SearchEngine-1.0-SNAPSHOT.jar`
    + в браузере перейти по адресу: `http://localhost:8080/`
+ В браузере работаем со вкладками как быдо описано выше
