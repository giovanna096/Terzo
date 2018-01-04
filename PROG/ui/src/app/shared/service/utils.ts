import { Injectable } from '@angular/core';

@Injectable()
export class Utils {

  /**
   * Promise with timeout
   * Source: https://italonascimento.github.io/applying-a-timeout-to-your-promises/
   */
  public static timeoutPromise = function (ms, promise) {
    // Create a promise that rejects in <ms> milliseconds
    let timeout = new Promise((resolve, reject) => {
      let id = setTimeout(() => {
        clearTimeout(id);
        reject('Timed out in ' + ms + 'ms.')
      }, ms)
    })

    // Returns a race between our timeout and the passed in promise
    return Promise.race([
      promise,
      timeout
    ])
  }

  constructor() { }

  /**
   * Helps to use an object inside an *ngFor loop. Returns the object keys.
   * Source: https://stackoverflow.com/a/39896058
   */
  public keys(object: {}): string[] {
    return Object.keys(object);
  }

  /**
   * Helps to use an object inside an *ngFor loop. Returns the object key value pairs.
   */
  public keyvalues(object: {}): any[] | {} {
    if (!object) {
      return object;
    }
    let keyvalues = [];
    for (let key in object) {
      keyvalues.push({ key: key, value: object[key] });
    }
    return keyvalues;
  }

  /**
   * Helps to use an object inside an *ngFor loop. Returns the object values.
   * Source: https://stackoverflow.com/a/39896058
   */
  public values(object: {}): any[] {
    let values = [];
    for (let key in object) {
      values.push(object[key]);
    }
    return values;
  }

  /**
   * Returns true if an object has a property
   */
  public has(object: {}, property: string): boolean {
    if (property in object) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Returns a sorted array
   */
  public sort(obj: any[], ascending: boolean = true, property?: string) {
    if (obj == null) {
      return obj;
    }
    return obj.sort((a, b) => {
      if (property) {
        a = a[property];
        b = b[property];
      }
      let result = 0;
      if (a > b) {
        result = 1;
      } else if (a < b) {
        result = -1;
      }
      if (!ascending) {
        result *= -1;
      }
      return result;
    })
  }

  /**
   * Returns the short classname
   */
  public classname(value): string {
    let parts = value.split(".");
    return parts[parts.length - 1];
  }

  /**
   * Creates a deep copy of the object
   */
  public deepCopy(obj) {
    var copy;

    // Handle the 3 simple types, and null or undefined
    if (null == obj || "object" != typeof obj) return obj;

    // Handle Date
    if (obj instanceof Date) {
      copy = new Date();
      copy.setTime(obj.getTime());
      return copy;
    }

    // Handle Array
    if (obj instanceof Array) {
      copy = [];
      for (var i = 0, len = obj.length; i < len; i++) {
        copy[i] = this.deepCopy(obj[i]);
      }
      return copy;
    }

    // Handle Object
    if (obj instanceof Object) {
      copy = {};
      for (var attr in obj) {
        if (obj.hasOwnProperty(attr)) copy[attr] = this.deepCopy(obj[attr]);
      }
      return copy;
    }

    throw new Error("Unable to copy obj! Its type isn't supported.");
  }

  public static addSafely(v1: number, v2: number): number {
    if (v1 == null) {
      return v2;
    } else if (v2 == null) {
      return v1;
    } else {
      return v1 + v2;
    }
  }

  public static subtractSafely(v1: number, v2: number): number {
    if (v1 == null) {
      return v2;
    } else if (v2 == null) {
      return v1;
    } else {
      return v1 - v2;
    }
  }

  public static divideSafely(v1: number, v2: number): number {
    if (v1 == null || v2 == null) {
      return null;
    } else {
      return v1 / v2;
    }
  }

  public static multiplySafely(v1: number, v2: number): number {
    if (v1 == null || v2 == null) {
      return null;
    } else {
      return v1 * v2;
    }
  }

  /**
   * Receive meta information for thing/channel/...
   */
  // TODO
  // public meta(identifier: string, type: 'controller' | 'channel'): {} {
  //   let property = type == 'controller' ? 'availableControllers' : type;
  //   let device = this.websocket.currentDevice;
  //   if (device) {
  //     let config = device.config.getValue();
  //     let meta = config._meta[property];
  //     if (identifier in meta) {
  //       return (meta[identifier]);
  //     }
  //   }
  //   return null;
  // }
}