FROM nginx:stable-alpine AS cert

RUN apk add --update openssl && \
    rm -rf /var/cache/apk/*

RUN openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout /nginx-selfsigned.key -out /nginx-selfsigned.crt -subj "/C=IT/ST=Modena/L=Modena/O=Quix S.r.l./OU=R&D/CN=localhost"
RUN openssl rsa -in /nginx-selfsigned.key -out /nginx-selfsigned.clean.key

FROM nginx:stable-alpine

ENV VAR_SYMBOL=$

COPY conf/default.conf.template /etc/nginx/conf.d/
COPY --from=cert /nginx-selfsigned.clean.key /etc/nginx/ssl/
COPY --from=cert /nginx-selfsigned.crt /etc/nginx/ssl/

EXPOSE 443

CMD ["nginx", "-g", "daemon off;"]

