# ПОИСКОВИК ПО САЙТАМ (SearchEngine)
_ _ _ _ _
### *SearchEngine* - представляет из себя поисковик по сайтам, который ищет на страницах этих сайтов необходимые русские слова или фразы.
_ _ _ _ _
### *Процесс поиска*
1. Перед поиском нужных слов, необходимо произвести индексацию сайтов (осуществить поис русских слов на каждой странице сайтов)
   + Для этого на вкладке MANAGEMENT нажать кнопку START INDEXING ![management](https://lh3.googleusercontent.com/drive-viewer/AK7aPaDyWlhtRHFr5A7qpHdzgE_I8G9e4DtGJfdvqDCt9Asc5aG2Z_gIgtgIEy_pjeVKjB8ZaDGcFUzEpi39nXTyUmNg7l4ZpQ=w1920-h935)
   + Результатом чего во вкладке DASHBOARD появится статистика о проиндексированных сайтах (количество найденых страниц, лемм и ошибках, возникших при индексации) ![DASHBOARD](https://lh3.googleusercontent.com/drive-viewer/AK7aPaCodRLefjJGavoTL4Lw2yajvhpkICfVJAww2uZwo1QiZRzg3XyjPZNAbdjDQOAHlo8WCpUvTNEm7lsPpO_Ka0ughtb1og=w1920-h935)
2. Во вкладке SEARCH выбрать сайт (все сайты), по которому будет производится поиcк, в поле *query* ввести поисковый запрос и нажать кнопку SEARCH. Получить результат ![reqwest](https://lh3.googleusercontent.com/drive-viewer/AK7aPaDPARIY0V3ltX5NBrBHyLYWDZDHCWLir3D76VUo5ZoUAMBKrDX69ePHLTVdLlGQHMHiRS5Yqpnl72YPIt0fITRpEoYZwg=w1920-h935)
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
+ Скачать файлы **SearchEngine-1.0-SNAPSHOT.jar** и **application.yaml** (в одну и туже папку)
+ В файле application.yaml изменить строки на ваши значения, используемые в **MySQLWorkbench**
     + username: ваше имя
     + password: ваш пароль
+ В командной строке:
    + перейти в директорию где хранится файл SearchEngine-1.0-SNAPSHOT.jar. Н-р `C:\Users\Иван> cd C:\Users\Иван\Desktop`
    + выполнить команду: `C:\Users\Иван\Desktop>java -jar SearchEngine-1.0-SNAPSHOT.jar`
    + в браузере перейти по адресу: `http://localhost:8080/`
+ В браузере работаем со вкладками как быдо описано выше
