# VK Web Chat
*Disclaimer: на эту работу было потрачено огромное количество сил, потому, если в случае 
отрицательного ответа вы оставите хотя бы отзыв в ишью, будет очень здорово*

## Как запускать

Этот проект тестировался и запускался на 11 и 16 Java.

Для запуска клиента `./gradlew runFrontend`

Для запуска сервера `./gradlew runBackend`

## Зависимости
В ТЗ было сказано использовать как можно меньше зависимостей, но так как я все-таки 
какие-то использовал, считаю нужным пояснить за каждую.

- Gson - все общение клиент-сервер у меня происходит именно через JSON.
    Json хорошо себя показывает в случае когда нужно соблюдать обратную совместимость.
    Например, добавить какое-то новое поле в респонс. Что позитивно сказывается на обратной
    совместимости. Есть конечно бинарные протоколы, но кажется, что они не так хорошо прижились
    в этом мире, что плохо скажется на адаптации проекта в случае если придется открывать апи.
- Jetty - http server. Я бы безусловно мог бы просто заюзать логику общения через LongPoll, но
    использование HTTP протокола кажется отличным решением за счет того что мы сразу же бесплатно
    можем использовать все штуки для него придуманные. HTTP прокси, https и прочее.  
    Jetty за меня занимается распределением нагрузки, но вам было бы наверно интересно 
    посмотреть как это делаю я. У меня это сделано в LongPoll.

Больше зависимостей нет. Только в тестах.

## Архитектура

В проектировании этого сервиса я в первую очередь руководствовался его непотопляемости 
во время больших нагрузок.

Большие нагрузки могут быть из-за двух вещей:
 1. Наш сервис безумно популярен 
 2. На нас ведется DDoS атака

Решение у двух этих вещей разное. Для борьбы с DDoS лучше всего отрубить атакующих 
нас товарищей на этапе фаервола, но так как это не так просто, лучшим решением будет 
сделать так что бы они нанесли наименьший вред. Прежде всего на софтварном уровне 
пытаться вычислять вредный трафик и его пресекать. Самый простой вариант это просто
трекать количество запросов. В моем случае, после N запросов я прошу решить человека 
математическое выражение. Оно передается текстом, но давайте притворимся, что это капча.
В какой-то момент я хотел сделать так что бы на клиент отправлялся client challenge. 
Например, разгадать хэш, но так как моя целевая аудитория это все таки человек, я решил 
что капча будет восприниматься лучше, но возможно имеет смысл совместить. 

В общем один IP адрес не сможет нам навредить много. То есть не больше N запросов в M минут,
но так как правило дудосят с бот нета IP адресов там достаточно много. Потому приходим
к второму фронту защиты: оптимизация потребляемых ресурсов и их утилизация. Так как 
этот фронт актуален и для случая когда наш сервис безумно популярен, можно тезисно
вынести направления, в которых я работал для выдерживания больших нагрузок:

1. Отсутствие уязвимостей, ведущих к утечкам памяти или процессорного времени
2. Уменьшение количества запросов необходимых для общения с сервером (но все еще можно было бы лучше).
    Тот же LongPoll чего только стоит
3. Уменьшение времени работы каждого запроса. Сейчас каждый эндпоинт делает не более одного запроса в БД
4. Утилизация процессорного времени. В проекте не используется блокирующее IO.

Сейчас вы не можете взять всю переписку целиком. Один запрос вернет не более 100 сообщений, но можно задавать временной 
промежуток. Такое же хотелось бы сделать для получения списка пользователей и друзей, но времени, к сожалению нет. 

Далее предлагается пройтись по каждому решению в отдельности

## Строгая типизация API

Хотя такого пункта не было в тз, но я очень горд как у меня здорово получилось.

В моей реализации каждый endpoint описывается через вот такую конструкцию:
```java
    public static final Endpoint<UserPasswordRequest, SessionResponse> SIGN_UP_REQUEST_ENDPOINT
            = new Endpoint<>("signup", UserPasswordRequest.class, SessionResponse.class);
```
После чего запросы выглядят вот так красиво
```java
        HttpClient client = connect();
        var response = client.sendRequest(
                Endpoint.SIGN_UP_REQUEST_ENDPOINT,
                new UserPasswordRequest(username, password)
        );
        SessionResponse session = assertSuccess(response.response());
        validateResponse(session);

```
Через этот же эндпоинт создается сервлет на сервере и в итоге у нас получается, что 
компилятор сам за нас проверят, что мы правильно общаемся с сервером. 

Считаю такие штуки очень важны в долгосрочной перспективе, когда протокол переписывается 
3 миллиона раз.

## Long poll
В задаче было написать чат в реальном времени. В таком случае главная проблема это то что
спасибо NAT'у батюшке мы не можем с сервера просто взять и послать клиенту обновление. 
Получается, что у нас клиент должен постоянно спрашивать на предмет новых сообщений.
Для обеспечения чата в "реальном" времени он должен делать это не реже 2-4 раз в секунду.

Для HTTP это огромные цифры потому что типичному HTTP запросу нужно сначала наладить TCP 
подключение, а потом отправить полноценный HTTP хедер. На самом деле даже без хедера это 
уже много. 

В общем именно по этой причине я использовал в своем решении LongPoll. Наиболее идеальным 
вариантом было бы реализовать WebSocket так в таком случае можно было бы общаться с веб 
страниц, но так как тащить либу для этого я посчитал читерством, а самому прописывать 
хедеры мне показалось бесполезной тратой времени для моей задачи, я реализовал LongPoll
на голом TCP. 

Далее так запросы к нашему LongPoll будут делать пару раз в секунду получается, что
если мы будем использовать отдельный поток на каждое подключение мы очень быстро
начнем терять много как памяти, так и процессора на это все дело. По сему использование 
блокирующего IO кажется плохим решением. 

Теперь надо рассмотреть проблемы дудоса, которые могут возникнуть.

Во-первых, так или иначе на каждое подключение мы выделяем какую-то память, не берусь 
сказать точно, но думаю не больше килобайта, из которых примерно все это TcpChannel.
При таком масштабе мы быстрее уткнемся в кол-во tcp портов чем почувствуем угрозу памяти,
но так как у каждого подключения еще существует очередь сообщений, размер каждого
из которых может доходить до 4096 байт, я кикаю подключения в случае переполнения 
очереди сообщений или слишком большого времени простоя.

Ну и важное уточнение, я не делал шифрования в подключение, хотя это и звучит как 
здравая мысля. Но я там отправляю туда-судя рандомные битики и реализовал хэндшейк. Так что в целом
можно было бы и шифрование устроить, но времени уже нет. Когда-то студентам надо учится,
а не только делать стажировки.

`Backend/src/main/java/ru/justagod/vk/backend/poll`

## Client challenge
В общем-то проще всего положить сервер для нормальных пользователей - это заполнить все 
процессорное время бесполезными запросами. Допустим у нас очень быстрые запросы, но 
бот-нет на 9 миллионов это все еще многовато. Потому самый простой варинат это замедлить 
подозрительный айпишник. Например, отправить его решать капчу. Да он все еще сможет нам 
слать запросы, но мы не будем ходить в бд и как следствие не будем тратить слишком много 
ЦПУ. Конечно, должно выполняться условие, что мы эту капчу решаем мгновенно.

Cloudflare любит отправлять клиентам решить JS задачку, но в моем случае я как то 
не смог придумать чем заменить такой способ. Хотел использовать разгадывание хешей, но потом вспомнил
про всякие радужные таблицы и решил, что лучше капчу.

Конечно я не сделал капчу в привычном понимании, а просто трекаю состояние пользователя и отправляю ему текстом 
задачку. Эдакий плейсхолдер.

`Backend/src/main/java/ru/justagod/vk/backend/dos`

## Database connections pooling

В проекте была использована SQLite. Это было сделано в угоду того что
удобство запуска важно. С таким же успехом можно было бы сделать запуск через 
Docker Compose, но как-то проще будет если просто притворимся,
что это какая-то адекватная база данных, к которой потом будут подключаться многие инстансы 
нашего сервиса.

Так вот, так как подключение к базе данных подразумевает как минимум TCP Handshake, 
открывать новое подключение при каждом запросе получается довольно дорого. Потому лучше
это дело хранить в памяти.

В случае с SQLite это конечно не имеет смысла.

`Backend/src/main/java/ru/justagod/vk/backend/db/ConnectionPool.java`

## Пароли

Пароли, конечно же хранятся в захэшированном виде.

## HTTPS
 
Оно определенно тут нужно. Мы тут шлем пароли как-никак. Я просто не хочу класть сюда 
сертификат никакой. Но честное пионерское я знаю как это делается в Jetty.

## Тесты

Про них сложно что-то рассказать. Тесты как тесты. Вот скрин покрытия:
![image](https://user-images.githubusercontent.com/20680875/161403581-7c5bbe63-5dd2-43f2-a38b-76561bcb4fc8.png)
