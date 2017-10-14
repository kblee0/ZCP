# ZCP

## 1. Introduction

ZCP(Zimbra Connector for POP3) is a POP3 service for Zimbra wemail.

If you want to use webmail with Outlook, you can install it on your PC and use it.

```
     ┌───────┐                 ┌───┐                 ┌───────┐
     │Outlook│                 │ZCP│                 │Webmail│
     └───┬───┘                 └─┬─┘                 └───┬───┘
         │Request POP3 protocol  │                       │    
         │──────────────────────>│                       │    
         │                       │                       │    
         │                       │Request Http Protocol  │    
         │                       │──────────────────────>│    
         │                       │                       │    
         │                       │Response Http Protocol │    
         │                       │<─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ │    
         │                       │                       │    
         │Response POP3 response │                       │    
         │<─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ │                       │    
     ┌───┴───┐                 ┌─┴─┐                 ┌───┴───┐
     │Outlook│                 │ZCP│                 │Webmail│
     └───────┘                 └───┘                 └───────┘
 ```

## 2. Limitation

ZCP only supports retrieving and downloading. In other words, deletion processing through POP3 is not deleted from actual webmail.



# 3. Interface Secenario

![ZCP Interface](https://raw.githubusercontent.com/kblee0/ZCP/master/ZCP_if.svg)


## 3. Configuration

### 3.1. config/pop3server.xml

- POP3 Server configuration

  - config/server/port : POP3 service port (eg. 110)

  - config/server/backlog: POP3 service socket listen backlog count (eg. 10)

  ```
   <server>
   	<port>80</port>
   	<backlog>10</backlog>
   </server>
  ```

- Mailbox folder configuration

  - config/folders/folder/path: webmail search query string (eg. in:inbox is:unread)
  - config/folders/folder/category (Optional): Category to enforce on mail. 

  ```
  <folders>
  	<folder>
  		<path>in:inbox</path>
  	</folder>
  	<folder>
  		<path>in:sent</path>
  		<category>SENT</category>
  	</folder>
  </folders>
  ```

- Zimbra webmail interface configuration

  - config/zimbra/serviceUrl: Zimbra webmail service url
  - config/zimbra/searchRequestLimit: Maximum number of message 
  - config/zimbra/messageIdStore: POP3 deleted message ID repository. (I'm using the sqlite3.)

  ```
  <zimbra>
  	<serviceUrl>https://www.zimbra.com/</serviceUrl>
  	<searchRequestLimit>500</searchRequestLimit>
  	<messageIdStore>config/UIDStore.db</messageIdStore>
  </zimbra>
  ```




# 4. Configuration samples

```
<?xml version="1.0" encoding="UTF-8" ?>
<config>
	<server>
		<port>80</port>
		<backlog>10</backlog>
	</server>
	<folders>
		<folder>
			<path>in:inbox</path>
		</folder>
		<folder>
			<path>in:sent</path>
			<category>SENT</category>
		</folder>
	</folders>
	<zimbra>
		<serviceUrl>https://www.zimbra.com/</serviceUrl>
		<searchRequestLimit>500</searchRequestLimit>
		<messageIdStore>config/UIDStore.db</messageIdStore>
	</zimbra>
</config>
```
