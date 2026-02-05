# Classed

## User
- id
- email
- username
- passwordhash
- createdAt


## UserActivity
- id
    - getUsername
- actionType
- createdAt

## UpMessage
- //update message

## Song
- str: name
- str: authors


# UnitBox
- date
- arrangement (how many unitboxs and how to arrange)
- boolean: isopen (whether church is open)


## SongsUnit(extends UnitBox)
- super(
    - date
    - boolean: operation (whether church is open)
)
- decidedSongs
- suggestedSongs
- topic



## PeopleUnit(extends UnitBox)
- super(
    - date
    - boolean: operation (whether church is open)
)
- dict: assignments = {"userid":instrument}




