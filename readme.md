# Online-Beratung MessageService

The MessageService acts as a middleman between the client and Rocket.Chat to read and write messages, respectively message streams.
It was introduced because Rocket.Chat did not provide the functionality of encrypting messages when this project started.

To provide the greatest possible security, the encryption key consists of three parts: a key saved within the service itself (or its configuration file), the session identification and a key that is not saved on the system itself (known as the masterkey). This masterkey must be provided after every restart of the MessageService. When this key constellation is changed old messages can't be decrypted anymore and new messages only can be en-/decrypted with this new set-up.

## Help and Documentation
In the project [documentation](https://onlinebertung.github.io/documentation/docs/setup/setup-backend) you'll find information for setting up and running the project.
You can find some detailled information of the service architecture and its processes in the repository [documentation](https://github.com/Onlineberatung/onlineBeratung-messageService/tree/master/documentation).

## License
The project is licensed under the AGPLv3 which you'll find [here](https://github.com/Onlineberatung/onlineBeratung-messageService/blob/master/LICENSE).

## Code of Conduct
Please have a look at our [Code of Conduct](https://github.com/Onlineberatung/.github/blob/master/CODE_OF_CONDUCT.md) before participating in the community.

## Contributing
Please read our [contribution guidelines](https://github.com/Onlineberatung/.github/blob/master/CONTRIBUTING.md) before contributing to this project.
