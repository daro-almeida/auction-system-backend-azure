import crypto from 'crypto';
import { faker } from '@faker-js/faker';
import axios, { AxiosResponse } from 'axios';

export interface CreateUserParams {
    nickname: string,
    name: string,
    password: string,
    imageBase64: string,
}

export interface CreateAuctionParams {
    title: string,
    description: string,
    userId: string,
    endTime: Date,
    minimumPrice: string,
    imageBase64: string,
}

export interface EndpointsConfig {
    baseUrl: string,
    mediaEndpoint?: string,
    userEndpoint?: string,
    auctionEndpoint?: string,
}

export type UploadMediaParams = ArrayBuffer;

export class Endpoints {
    readonly base: string
    readonly media: string
    readonly user: string
    readonly auction: string

    constructor(config: EndpointsConfig) {
        this.base = config.baseUrl;
        this.media = `${this.base}/${(config.mediaEndpoint || 'rest/media')}`;
        this.user = `${this.base}/${(config.userEndpoint || 'rest/user')}`;
        this.auction = `${this.base}/${(config.auctionEndpoint || 'rest/auction')}`;
    }
}

export class Requests {
    private validate: boolean;
    private endpoints: Endpoints;

    constructor(endpoints: Endpoints, validate?: boolean) {
        this.endpoints = endpoints;
        this.validate = validate || false;
    }

    async createUser(params: CreateUserParams): Promise<AxiosResponse<any, any>> {
        return await axios({
            method: 'post',
            url: `${this.endpoints.user}`,
            data: params,
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
            },
            validateStatus: this.validator(),
        });
    }

    async uploadMedia(params: UploadMediaParams): Promise<AxiosResponse<any, any>> {
        return await axios({
            method: 'post',
            url: `${this.endpoints.media}`,
            data: params,
            headers: {
                'Content-Type': 'application/octet-stream',
                'Accept': 'application/json',
            },
            validateStatus: this.validator(),
        });
    }

    async downloadMedia(id: string): Promise<AxiosResponse<any, any>> {
        return await axios({
            method: 'get',
            url: `${this.endpoints.media}/${id}`,
            responseType: 'arraybuffer',
            validateStatus: this.validator(),
        });
    }

    async createAuction(params: CreateAuctionParams): Promise<AxiosResponse<any, any>> {
        return await axios({
            method: 'post',
            url: `${this.endpoints.auction}`,
            data: params,
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
            },
            validateStatus: this.validator(),
        });
    }

    private validator(): (status: number) => boolean {
        if (this.validate)
            return (status) => { return status >= 200 && status < 300 };
        else
            return (status) => { return true };
    }
}

export class Client {
    private endpoints: Endpoints;
    private requests: Requests;

    constructor(endpoints: Endpoints) {
        this.endpoints = endpoints;
        this.requests = new Requests(endpoints);
    }

    /**
     * Create a new user
     * @param params User creation parameters
     * @returns User ID of the created user
     */
    async createUser(params: CreateUserParams): Promise<string> {
        return (await this.requests.createUser(params)).data;
    }

    /**
     * Upload a media file
     * @param params Media to upload
     * @returns Media ID of the uploaded media
     */
    async uploadMedia(params: UploadMediaParams): Promise<string> {
        return (await this.requests.uploadMedia(params)).data;
    }
}

export function randomCreateUserParams(): CreateUserParams {
    return {
        nickname: faker.internet.userName(),
        name: faker.name.fullName(),
        password: faker.internet.password(),
        imageBase64: crypto.randomBytes(64 * 1024).toString('base64'),
    }
}

export function randomCreateAuctionParams(userId: string): CreateAuctionParams {
    return {
        title: `${faker.commerce.productAdjective()} ${faker.commerce.productMaterial()} ${faker.commerce.product()}`,
        description: faker.lorem.paragraph(),
        userId: userId,
        endTime: faker.date.future(),
        minimumPrice: faker.commerce.price(),
        imageBase64: crypto.randomBytes(64 * 1024).toString('base64'),
    }
}

export function randomUploadMediaParams(): ArrayBuffer {
    return crypto.randomBytes(64 * 1024);
}